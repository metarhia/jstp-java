package com.metarhia.jstp.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.metarhia.jstp.Constants;
import com.metarhia.jstp.TestConstants;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSSerializer;
import com.metarhia.jstp.core.JSTypes.IndexedHashMap;
import com.metarhia.jstp.exceptions.ConnectionException;
import com.metarhia.jstp.handlers.CallHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentMatcher;
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

  private AbstractSocket transport;

  public ConnectionTest() {
    instance = this;
    transport = mock(AbstractSocket.class);
    connection = spy(new Connection(transport));
    connection.getSessionPolicy().setConnection(connection);
    doAnswer(new HandshakeAnswer(connection)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_REQUEST));
    when(transport.isConnected()).thenReturn(true);
    connection.handshake(TestConstants.MOCK_APP_NAME, null);
  }

  @AfterAll
  public static void tearDown() {
    if (instance != null && instance.connection != null) {
      instance.connection.close();
      instance.connection = null;
    }
  }

  private static ConnectionSpy createConnectionSpy() {
    return createConnectionSpy(null, true);
  }

  private static ConnectionSpy createConnectionSpy(AbstractSocket transport,
                                                   boolean transportConnected) {
    if (transport == null) {
      transport = mock(AbstractSocket.class);
      when(transport.isConnected()).thenReturn(transportConnected);
    }
    Connection connection = spy(new Connection(transport));
    connection.getSessionPolicy().setConnection(connection);
    return new ConnectionSpy(connection, transport);
  }

  @Test
  public void pingPong() throws Exception {
    String input = "{ping:[42]}";
    String response = "{pong:[42]}";

    connection.onMessageReceived(JSParser.<JSObject>parse(input));

    verify(transport, times(1))
        .send(argThat(new MessageMatcher(response)));
  }

  @Test
  public void illFormedMessages() throws Exception {
    ConnectionListener listener = spy(ConnectionListener.class);
    connection.addSocketListener(listener);
    JSParser parser = new JSParser();
    for (Map.Entry<String, List<String>> illList : illFormedMessages.entrySet()) {
      for (String messageString : illList.getValue()) {
        parser.setInput(messageString);
        final JSObject message = parser.parse();
        connection.onMessageReceived(message);

        verify(listener, times(1)).onMessageRejected(message);

        connection.handshake(TestConstants.MOCK_APP_NAME, null);
      }
    }
  }

  @Test
  public void callbackHandler() throws Exception {
    long messageNumber = 42;
    String methodName = "method";
    String interfaceName = "interfaceName";
    final List<?> recvArgs = Collections.singletonList(null);
    final List<?> responseArgs = Arrays.asList(24, "whatever");

    final String input = String.format(TestConstants.TEMPLATE_CALL,
        messageNumber, interfaceName, methodName, JSSerializer.stringify(recvArgs));
    JSObject call = JSParser.parse(input);
    connection.setCallHandler(interfaceName, methodName, new CallHandler() {
      @Override
      public void handleCallback(List<?> data) {
        assertEquals(data, recvArgs);
        callback(connection, JSCallback.OK, responseArgs);
      }
    });
    connection.onMessageReceived(call);
    verify(connection, times(1))
        .callback(JSCallback.OK, responseArgs, messageNumber);
  }

  @Test
  public void handshakeSendRecv() throws Exception {
    ConnectionSpy cs = createConnectionSpy();
    AbstractSocket transport = cs.transport;
    Connection connection = cs.connection;

    doAnswer(new HandshakeAnswer(connection)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_REQUEST));

    connection.handshake(TestConstants.MOCK_APP_NAME, null);

    assertTrue(connection.isConnected());
  }

  @Test
  public void handshakeError() throws Exception {
    final int errorCode = 16;
    ConnectionSpy cs = createConnectionSpy();
    AbstractSocket transport = cs.transport;
    final Connection connection = cs.connection;

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String response = String.format(TestConstants.MOCK_HANDSHAKE_RESPONSE_ERR, errorCode);
        JSObject errMessage = JSParser.parse(response);
        connection.onMessageReceived(errMessage);
        return null;
      }
    }).when(transport).send(matches(TestConstants.ANY_HANDSHAKE_REQUEST));

    ConnectionListener listener = mock(ConnectionListener.class);
    connection.addSocketListener(listener);

    connection.handshake(TestConstants.MOCK_APP_NAME, null);

    verify(listener, times(1)).onConnectionError(argThat(
        new ArgumentMatcher<ConnectionException>() {
          @Override
          public boolean matches(ConnectionException error) {
            return error.getErrorCode() == errorCode;
          }
        }));
    assertTrue(connection.getSessionId() == null);
    assertFalse(connection.isConnected());
  }

  @Test
  void handshakeWithSession() throws Exception {
    String appName = "testApp";
    String sessionId = "sessionId";

    ConnectionSpy cs = createConnectionSpy();
    AbstractSocket transport = cs.transport;
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

    ConnectionSpy cs = createConnectionSpy();
    AbstractSocket transport = cs.transport;
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

    ConnectionSpy cs = createConnectionSpy();
    AbstractSocket transport = cs.transport;
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
        AbstractSocket abstractSocket = mock(AbstractSocket.class);
        Connection connection = new Connection(abstractSocket);
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
        AbstractSocket abstractSocket = mock(AbstractSocket.class);
        Connection connection = new Connection(abstractSocket);
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
      connection.onMessageReceived(message);
      connection.removeCallHandler(ba.interfaceName, ba.methodName);

      verify(handler, times(1)).handle(message);
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
      connection.onMessageReceived(message);
      connection.removeEventHandler(ba.interfaceName, ba.methodName, handler);

      verify(handler, times(1)).handle(message);
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
    ManualHandler handler = spy(ManualHandler.class);
    for (CallbackArguments cba : callbackMessages) {
      String args = JSSerializer.stringify(cba.args);
      String callString = String.format(TestConstants.TEMPLATE_CALLBACK,
          cba.messageNumber, cba.callback, args);
      final JSObject message = JSParser.parse(callString);

      connection.addHandler(cba.messageNumber, handler);
      connection.onMessageReceived(message);

      verify(handler, times(1)).handle(message);
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

      connection.onMessageReceived(JSParser.<JSObject>parse(inspectRequest));

      JSCallback callback = JSCallback.OK;
      List<?> args = ia.methods;
      if (args == null) {
        callback = JSCallback.ERROR;
        args = Collections.singletonList(Constants.ERR_INTERFACE_NOT_FOUND);
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

    connection.onMessageReceived(heartbeat);

    // must not reject heartbeat packet
    verify(listener, times(0))
        .onMessageRejected(ArgumentMatchers.<JSObject>notNull());
  }

  @Test
  void useTransportNotConnected() {
    ConnectionSpy cs = createConnectionSpy(null, false);
    AbstractSocket transport = cs.transport;
    Connection connection = cs.connection;

    connection.connect("testApp");

    AbstractSocket anotherTransport = mock(AbstractSocket.class);
    when(anotherTransport.isConnected()).thenReturn(false);
    connection.useTransport(anotherTransport);

    verify(anotherTransport, times(1)).connect();
  }

  @Test
  void useTransportConnected() throws Exception {
    String appName = "testApp";

    ConnectionSpy cs = createConnectionSpy(null, false);
    AbstractSocket transport = cs.transport;
    Connection connection = cs.connection;

    // to set app name
    connection.connect(appName);

    AbstractSocket anotherSocket = mock(AbstractSocket.class);
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
    connection.onMessageReceived(JSParser.<JSObject>parse(callString));

    verify(handler, times(0)).handle(any(JSObject.class));
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
    connection.onMessageReceived(JSParser.<JSObject>parse(callString));

    verify(handler, times(0)).handle(any(JSObject.class));
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

  private static class ConnectionSpy {

    public Connection connection;

    public AbstractSocket transport;

    public ConnectionSpy(Connection connection, AbstractSocket transport) {
      this.connection = connection;
      this.transport = transport;
    }
  }

}
