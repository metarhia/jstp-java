package com.metarhia.jstp.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.metarhia.jstp.TestConstants;
import com.metarhia.jstp.TestUtils;
import com.metarhia.jstp.TestUtils.ConnectionSpy;
import com.metarhia.jstp.connection.Connection.ReconnectCallback;
import com.metarhia.jstp.connection.Connection.TransportConnector;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSSerializer;
import com.metarhia.jstp.core.JSTypes.IndexedHashMap;
import com.metarhia.jstp.core.JSTypes.JSElements;
import com.metarhia.jstp.handlers.OkErrorHandler;
import com.metarhia.jstp.transport.Transport;
import com.metarhia.jstp.transport.Transport.TransportListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentMatchers;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ConnectionTest {

  private static final List<BasicArguments> callMessages = Arrays.asList(
      new BasicArguments("iface3", "method", Collections.EMPTY_LIST),
      new BasicArguments("iface2", "method2", Collections.singletonList(13)),
      new BasicArguments("iface", "method3", Arrays.asList(1, "abc", 42)));

  private static final List<CallbackArguments> callbackMessages = Arrays.asList(
      new CallbackArguments(13, JSCallback.OK, Collections.EMPTY_LIST),
      new CallbackArguments(11, JSCallback.OK, Collections.singletonList(13)),
      new CallbackArguments(42, JSCallback.OK, Arrays.asList(1, "abc", 42)),
      new CallbackArguments(43, JSCallback.ERROR, Arrays.asList(1, "Remote error")));

  private static final List<BasicArguments> eventMessages = callMessages;

  private static final List<InspectArguments> inspectMessages = Arrays.asList(
      new InspectArguments(11, "iface1",
          Arrays.asList("method")),
      new InspectArguments(11, "iface2",
          Arrays.asList("method1, method2")),
      new InspectArguments(13, "auth",
          null));

  private static final Map<String, List<String>> illFormedMessages = new HashMap<>();

  static {
    illFormedMessages.put("Call message", Arrays.asList(
        "{call:['ss','auth'],newAccount:{a: 'Payload data'}}",
        "{call:[17,14],newAccount:['Payload data']}",
        "{call:[17,'auth']}"));
    illFormedMessages.put("Callback message", Arrays.asList(
        "{callback:['ss'],ok:[15703]}"));

    illFormedMessages.put("Event message", Arrays.asList(
        "{event:['chat'],message:['Marcus','Hello there!']}",
        "{event:[-12,'chat']}"));

//    illFormedMessages.put("Handshake request message", Arrays.asList(
//        "{handshake:[0],marcus:'7b458e1a9dda67cb7a3e'}",
//        "{handshake:[0,'example']}",
//        "{handshake:[0,'example'],marcus:1111}",
//        "{handshake:[0,'example'],marcus:'7b458e1a9dda67cb7a3e'}"
//    ));

    illFormedMessages.put("Handshake response message", Arrays.asList(
        "{handshake:['xx'],ok:'9b71d224bd62bcdec043'}",
        "{handshake:[0],ok:333}"));

    illFormedMessages.put("inspect request message", Arrays.asList(
        "{inspect:['22','interfaceName']}",
        "{inspect:[42]}"));

    illFormedMessages.put("inspect response message", Arrays.asList(
        "{callback:[42]}",
        "{callback:[42],ok:{'method1':'method2'}}"));
  }

  private static ConnectionTest instance;

  @Spy
  private Connection connection;

  private Transport transport;

  public ConnectionTest() {
    instance = this;
    ConnectionSpy cs = TestUtils.createConnectionSpy();
    transport = cs.transport;
    connection = cs.connection;
    doAnswer(new HandshakeAnswer(connection)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_REQUEST));
    connection.handshake(TestConstants.MOCK_APP_NAME, null);
  }

  @AfterAll
  public static void tearDown() {
    if (instance != null && instance.connection != null) {
      instance.connection.close();
      instance.connection = null;
    }
  }

  @Test
  public void pingPong() throws Exception {
    OkErrorHandler handler = mock(OkErrorHandler.class);
    doCallRealMethod().when(handler)
        .onMessage(any(JSObject.class));

    Answer loopbackAnswer = new LoopbackAnswer(connection);
    doAnswer(loopbackAnswer)
        .when(transport).send(matches(TestConstants.ANY_PING));
    doAnswer(loopbackAnswer)
        .when(transport).send(matches(TestConstants.ANY_PONG));

    connection.ping(handler);

    verify(handler, times(1))
        .handleOk(Collections.EMPTY_LIST);
  }

  @Test
  public void illFormedMessages() throws Exception {
    ConnectionListener listener = spy(ConnectionListener.class);
    connection.addListener(listener);
    JSParser parser = new JSParser();
    for (Map.Entry<String, List<String>> illList : illFormedMessages.entrySet()) {
      for (String messageString : illList.getValue()) {
        parser.setInput(messageString);
        final JSObject message = parser.parse();
        connection.onMessageParsed(message);

        verify(listener, times(1)).onMessageRejected(message);

        connection.handshake(TestConstants.MOCK_APP_NAME, null);
      }
    }
  }

  @Test
  public void handshakeSendRecv() throws Exception {
    ConnectionSpy cs = TestUtils.createConnectionSpy();
    Transport transport = cs.transport;
    Connection connection = cs.connection;

    ConnectionListener listener = mock(ConnectionListener.class);
    connection.addListener(listener);

    doAnswer(new HandshakeAnswer(connection)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_REQUEST));

    connection.handshake(TestConstants.MOCK_APP_NAME, null);

    verify(listener, times(1))
        .onConnected(false);
    assertTrue(connection.isConnected());
  }

  @Test
  public void handshakeError() throws Exception {
    final int errorCode = 16;
    ConnectionSpy cs = TestUtils.createConnectionSpy();
    Transport transport = cs.transport;
    final Connection connection = cs.connection;
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        connection.onTransportClosed();
        return null;
      }
    }).when(transport).close(anyBoolean());

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String response = String.format(TestConstants.MOCK_HANDSHAKE_RESPONSE_ERR, errorCode);
        JSObject errMessage = JSParser.parse(response);
        connection.onMessageParsed(errMessage);
        return null;
      }
    }).when(transport).send(matches(TestConstants.ANY_HANDSHAKE_REQUEST));

    ConnectionListener listener = mock(ConnectionListener.class);
    OkErrorHandler handler = spy(OkErrorHandler.class);

    connection.addListener(listener);

    connection.handshake(TestConstants.MOCK_APP_NAME, handler);

    verify(handler, times(1))
        .handleError(eq(errorCode), anyList());
    verify(handler, never())
        .handleOk(anyList());

    verify(listener, times(1))
        .onConnectionClosed();

    assertTrue(connection.getSessionId() == null);
    assertFalse(connection.isConnected());
  }

  @Test
  void handshakeWithSession() throws Exception {
    String appName = "testApp";
    String sessionId = "sessionId";

    ConnectionSpy cs = TestUtils.createConnectionSpy();
    Transport transport = cs.transport;
    Connection connection = cs.connection;

    connection.handshake(appName, sessionId, null);

    String request = String.format(TestConstants.TEMPLATE_HANDSHAKE_RESTORE_REQUEST,
        appName, sessionId, 0);
    verify(transport, times(1))
        .send(argThat(new MessageMatcher(request)));
  }

  @Test
  void handshakeWithVersion() throws Exception {
    AppData app = new AppData("appName", "1.0.0");

    ConnectionSpy cs = TestUtils.createConnectionSpy();
    Transport transport = cs.transport;
    Connection connection = cs.connection;

    connection.handshake(app, null);

    String request = String.format(TestConstants.TEMPLATE_HANDSHAKE_VERSION_REQUEST,
        app.getName(), app.getVersion());
    verify(transport, times(1))
        .send(argThat(new MessageMatcher(request)));
  }

  @Test
  void handshakeWithVersionRange() throws Exception {
    AppData app = new AppData("appName", "^1.1.0");

    ConnectionSpy cs = TestUtils.createConnectionSpy();
    Transport transport = cs.transport;
    Connection connection = cs.connection;

    connection.handshake(app, null);

    String request = String.format(TestConstants.TEMPLATE_HANDSHAKE_VERSION_REQUEST,
        app.getName(), app.getVersion());
    verify(transport, times(1))
        .send(argThat(new MessageMatcher(request)));
  }

  @Test
  void connectNullApp() {
    Throwable exception = assertThrows(RuntimeException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        Transport transport = mock(Transport.class);
        Connection connection = new Connection(transport);
        connection.connect((AppData) null, null);
      }
    });
    assertEquals("Application must not be null", exception.getMessage());
  }

  @Test
  void handshakeNullApp() {
    Throwable exception = assertThrows(RuntimeException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        Transport transport = mock(Transport.class);
        Connection connection = new Connection(transport);
        connection.handshake((AppData) null, null);
      }
    });
    assertEquals("Application must not be null", exception.getMessage());
  }

  @Test
  void call() throws Exception {
    for (BasicArguments ba : callMessages) {
      String args = JSSerializer.stringify(ba.args);
      String callString = String.format(TestConstants.TEMPLATE_CALL,
          0, ba.interfaceName, ba.methodName, args);

      connection.call(ba.interfaceName, ba.methodName, ba.args, null);

      verify(transport, times(1))
          .send(argThat(new MessageMatcherNoCount(callString)));
    }
  }

  @Test
  void callHandling() throws Exception {
    ManualHandler handler = spy(ManualHandler.class);
    for (BasicArguments ba : callMessages) {
      String args = JSSerializer.stringify(ba.args);
      String callString = String.format(TestConstants.TEMPLATE_CALL,
          0, ba.interfaceName, ba.methodName, args);
      final JSObject message = JSParser.parse(callString);

      connection.setCallHandler(ba.interfaceName, ba.methodName, handler);
      connection.onMessageParsed(message);
      connection.removeCallHandler(ba.interfaceName, ba.methodName);

      verify(handler, times(1)).onMessage(message);
    }
  }

  @Test
  void event() throws Exception {
    for (BasicArguments ba : eventMessages) {
      String args = JSSerializer.stringify(ba.args);
      String eventString = String.format(TestConstants.TEMPLATE_EVENT,
          0, ba.interfaceName, ba.methodName, args);

      connection.event(ba.interfaceName, ba.methodName, ba.args);

      verify(transport, times(1))
          .send(argThat(new MessageMatcherNoCount(eventString)));
    }
  }

  @Test
  void eventHandling() throws Exception {
    ManualHandler handler = spy(ManualHandler.class);
    for (BasicArguments ba : eventMessages) {
      String args = JSSerializer.stringify(ba.args);
      String callString = String.format(TestConstants.TEMPLATE_EVENT,
          0, ba.interfaceName, ba.methodName, args);
      final JSObject message = JSParser.parse(callString);

      connection.addEventHandler(ba.interfaceName, ba.methodName, handler);
      connection.onMessageParsed(message);
      connection.removeEventHandler(ba.interfaceName, ba.methodName, handler);

      verify(handler, times(1)).onMessage(message);
    }
  }

  @Test
  void callback() throws Exception {
    for (CallbackArguments cba : callbackMessages) {
      String args = JSSerializer.stringify(cba.args);
      String callbackString = String.format(TestConstants.TEMPLATE_CALLBACK,
          cba.messageNumber, cba.callback, args);

      connection.callback(cba.callback, cba.args, cba.messageNumber);

      verify(transport, times(1))
          .send(argThat(new MessageMatcher(callbackString)));
    }
  }

  @Test
  void callbackHandling() throws Exception {
    OkErrorHandler handler = spy(OkErrorHandler.class);
    for (CallbackArguments cba : callbackMessages) {
      String args = JSSerializer.stringify(cba.args);
      String callString = String.format(TestConstants.TEMPLATE_CALLBACK,
          cba.messageNumber, cba.callback, args);
      final JSObject message = JSParser.parse(callString);

      connection.addHandler(cba.messageNumber, handler);
      connection.onMessageParsed(message);

      verify(handler, times(1)).onMessage(message);
    }
  }

  @Test
  public void inspect() throws Exception {
    for (InspectArguments ia : inspectMessages) {

      connection.inspect(ia.interfaceName, null);

      String inspectRequest = String.format(TestConstants.TEMPLATE_INSPECT,
          ia.messageNumber, ia.interfaceName);
      verify(transport, times(1))
          .send(argThat(new MessageMatcherNoCount(inspectRequest)));
    }
  }

  @Test
  public void inspectHandler() throws Exception {
    for (InspectArguments ia : inspectMessages) {
      if (ia.methods != null) {
        connection.setClientMethodNames(ia.interfaceName, ia.methods);
      }
      String inspectRequest = String.format(TestConstants.TEMPLATE_INSPECT,
          ia.messageNumber, ia.interfaceName);

      connection.onMessageParsed(JSParser.<JSObject>parse(inspectRequest));

      JSCallback callback = JSCallback.OK;
      List<?> args = ia.methods;
      if (args == null) {
        callback = JSCallback.ERROR;
        args = Collections.singletonList(ConnectionError.INTERFACE_NOT_FOUND.getErrorCode());
      }
      String response = String.format(TestConstants.TEMPLATE_CALLBACK,
          ia.messageNumber, callback, JSSerializer.stringify(args));
      verify(transport, times(1))
          .send(argThat(new MessageMatcher(response)));
    }
  }

  @Test
  public void heartbeat() throws Exception {
    JSObject heartbeat = new IndexedHashMap();
    ConnectionListener listener = mock(ConnectionListener.class);

    connection.onMessageParsed(heartbeat);

    // must not reject heartbeat packet
    verify(listener, times(0))
        .onMessageRejected(ArgumentMatchers.<JSObject>notNull());
  }

  @Test
  void notNullAppData() {
    Connection connection = TestUtils.createConnectionSpy().connection;

    assertNotNull(connection.getAppData(), "AppData must not be null");
  }

  @Test
  void useTransportNoHandshake() {
    Connection connection = TestUtils.createConnectionSpy().connection;

    Transport anotherTransport = mock(Transport.class);
    when(anotherTransport.isConnected()).thenReturn(false);
    connection.useTransport(anotherTransport);

    verify(anotherTransport, never()).connect();
  }

  @Test
  void useTransportNotConnected() {
    ConnectionSpy cs = TestUtils.createConnectionSpy(null, false);
    Transport transport = cs.transport;
    Connection connection = cs.connection;

    connection.connect("testApp");

    Transport anotherTransport = mock(Transport.class);
    when(anotherTransport.isConnected()).thenReturn(false);
    connection.useTransport(anotherTransport);

    verify(anotherTransport, times(1)).connect();
  }

  @Test
  void useTransportConnected() throws Exception {
    String appName = "testApp";

    ConnectionSpy cs = TestUtils.createConnectionSpy(null, false);
    Transport transport = cs.transport;
    Connection connection = cs.connection;

    // to set app name
    connection.connect(appName);

    Transport anotherSocket = mock(Transport.class);
    when(anotherSocket.isConnected()).thenReturn(true);

    connection.useTransport(anotherSocket);

    String request = String.format(TestConstants.TEMPLATE_HANDSHAKE_REQUEST, appName);
    verify(anotherSocket, times(1))
        .send(argThat(new MessageMatcher(request)));
  }

  @Test
  void removeCallHandler() throws Exception {
    final String interfaceName = "iface";
    final String methodName = "method";

    final ManualHandler handler = spy(ManualHandler.class);
    connection.setCallHandler(interfaceName, methodName, handler);
    connection.removeCallHandler(interfaceName, methodName);

    String callString =
        String.format(TestConstants.TEMPLATE_CALL, 13, interfaceName, methodName, "[]");
    connection.onMessageParsed(JSParser.<JSObject>parse(callString));

    verify(handler, times(0)).onMessage(any(JSObject.class));
  }

  @Test
  void removeEventHandler() throws Exception {
    final String interfaceName = "iface";
    final String methodName = "method";

    final ManualHandler handler = spy(ManualHandler.class);
    connection.addEventHandler(interfaceName, methodName, handler);
    connection.removeEventHandler(interfaceName, methodName, handler);

    String callString =
        String.format(TestConstants.TEMPLATE_EVENT, 13, interfaceName, methodName, "[]");
    connection.onMessageParsed(JSParser.<JSObject>parse(callString));

    verify(handler, times(0)).onMessage(any(JSObject.class));
  }

  @Test
  void removeListenerFromCallback() {
    final ConnectionSpy cs = TestUtils.createConnectionSpy();
    final Connection connection = cs.connection;
    doAnswer(new HandshakeAnswer(connection)).when(cs.transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_REQUEST));
    SimpleConnectionListener listener = spy(new SimpleConnectionListener() {
      @Override
      public void onConnected(boolean restored) {
        connection.removeListener(this);
      }
    });
    connection.addListener(listener);

    connection.connect(TestConstants.MOCK_APP_NAME);

    verify(listener, times(1))
        .onConnected(false);
  }

  @Test
  void multipleConnectMustReportClosed() throws Exception {
    final int connectCalls = 3;
    final CountDownLatch latch = new CountDownLatch(connectCalls);
    final ConnectionSpy cs = TestUtils.createConnectionSpy(null, false);
    final Transport transport = cs.transport;
    final Connection connection = cs.connection;
    when(transport.connect()).then(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            connection.onTransportClosed();
          }
        }).start();
        return true;
      }
    });
    final ConnectionListener listener = spy(ConnectionListener.class);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) {
        latch.countDown();
        if (latch.getCount() > 0) {
          connection.connect(TestConstants.MOCK_APP_NAME);
        }
        return null;
      }
    }).when(listener).onConnectionClosed();
    connection.addListener(listener);

    connection.connect(TestConstants.MOCK_APP_NAME);

    latch.await(10, TimeUnit.SECONDS);
    verify(listener, times(connectCalls))
        .onConnectionClosed();
  }

  @Test
  void reconnectCallbackDefault() {
    final ConnectionSpy cs = TestUtils.createConnectionSpy();
    final Connection connection = cs.connection;
    final Transport transport = cs.transport;
    doAnswer(new HandshakeAnswer(connection)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_REQUEST));

    ReconnectCallback reconnectCallback = spy(new ReconnectCallback() {
      @Override
      public void onConnectionLost(Connection connection,
                                   TransportConnector transportConnector) {
        // must connect to the same transport
        transportConnector.connect(null);
      }
    });
    connection.setReconnectCallback(reconnectCallback);

    connection.connect(TestConstants.MOCK_APP_NAME);

    TestUtils.simulateDisconnect(connection, transport);

    verify(reconnectCallback, times(1))
        .onConnectionLost(isA(Connection.class), isA(TransportConnector.class));
    verify(transport, times(1))
        .connect();
  }

  @Test
  void reconnectCallbackNewTransport() {
    final ConnectionSpy cs = TestUtils.createConnectionSpy();
    final Connection connection = cs.connection;
    final Transport transport = cs.transport;

    doAnswer(new HandshakeAnswer(connection)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_REQUEST));

    final Transport newTransport = mock(Transport.class);
    ReconnectCallback reconnectCallback = spy(new ReconnectCallback() {
      @Override
      public void onConnectionLost(Connection connection,
                                   TransportConnector transportConnector) {
        // must use the new transport
        transportConnector.connect(newTransport);
      }
    });
    connection.setReconnectCallback(reconnectCallback);

    connection.connect(TestConstants.MOCK_APP_NAME);

    TestUtils.simulateDisconnect(connection, transport);

    verify(reconnectCallback, times(1))
        .onConnectionLost(isA(Connection.class), isA(TransportConnector.class));
    verify(transport, never())
        .connect();
    verify(newTransport, times(1))
        .connect();
  }

  @Test
  void messageHandler() throws Exception {
    OkErrorHandler handler = mock(OkErrorHandler.class);
    connection.addHandler(13, handler);

    ((TransportListener) connection).onMessageReceived("{pong:[13]}");

    synchronized (ConnectionTest.this) {
      // messages are parsed in a separate thread so wait a bit to make sure
      // we have finished parsing
      wait(500);
    }

    verify(handler, times(1))
        .onMessage(JSElements.EMPTY_OBJECT);
  }

  private static class BasicArguments {

    String interfaceName;
    String methodName;
    List<?> args;

    public BasicArguments(String interfaceName, String methodName, List<?> args) {
      this.interfaceName = interfaceName;
      this.methodName = methodName;
      this.args = args;
    }
  }

  private static class CallbackArguments {

    long messageNumber;
    JSCallback callback;
    List<?> args;

    public CallbackArguments(long messageNumber, JSCallback callback, List<?> args) {
      this.messageNumber = messageNumber;
      this.callback = callback;
      this.args = args;
    }
  }

  private static class InspectArguments {

    long messageNumber;
    String interfaceName;
    List<String> methods;

    public InspectArguments(long messageNumber, String interfaceName,
                            List<String> methods) {
      this.messageNumber = messageNumber;
      this.interfaceName = interfaceName;
      this.methods = methods;
    }
  }

}
