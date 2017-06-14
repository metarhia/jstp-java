package com.metarhia.jstp.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.isA;
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
import com.metarhia.jstp.handlers.CallHandler;
import com.metarhia.jstp.storage.FileStorage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
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
    doAnswer(new HandshakeAnswer(connection)).when(connection)
        .handshake(anyString(), isA(ManualHandler.class));
    doAnswer(new HandshakeAnswer(connection)).when(connection)
        .handshake(anyString(), Mockito.<ManualHandler>isNull());
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

  @Test
  public void checkPingPong() throws Exception {
    String input = "{ping:[42]}" + Connection.TERMINATOR;
    String response = "{pong:[42]}" + Connection.TERMINATOR;

    connection.onMessageReceived(JSParser.<JSObject>parse(input));

    verify(transport, times(1))
        .send(argThat(new MessageMatcher(response)));
  }

  @Test
  public void saveRestoreSession() throws Exception {
    String folder = "/tmp";
    FileStorage storage = new FileStorage(folder);

    connection.saveSession(storage);

    AbstractSocket socket = mock(AbstractSocket.class);
    Connection anotherConn = new Connection(socket);

    anotherConn.restoreSession(storage);

    assertTrue(connection.getSessionData().equals(anotherConn.getSessionData()));
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
    final List<?> recvArgs = new ArrayList<>();
    recvArgs.add(null);
    final List<?> responseArgs = Arrays.asList(24, "whatever");

    final String input =
        String.format("{call:[%d,'%s'], %s:%s}", messageNumber, interfaceName,
            methodName, JSSerializer.stringify(recvArgs));
    JSObject callback = JSParser.parse(input);
    connection.setCallHandler(interfaceName, "method", new CallHandler() {
      @Override
      public void handleCallback(List<?> data) {
        assertEquals(data, recvArgs);
        callback(connection, JSCallback.OK, responseArgs);
      }
    });
    connection.onMessageReceived(callback);
    verify(connection, times(1))
        .callback(JSCallback.OK, responseArgs, messageNumber);
  }

  @Test
  public void restorationPolicyCheck() throws Exception {
    final boolean[] success = {false, false};

    AbstractSocket transport = mock(AbstractSocket.class);
    when(transport.isConnected()).thenReturn(true);

    Connection connection = spy(new Connection(transport, new RestorationPolicy() {
      @Override
      public boolean restore(Connection connection, Queue<Message> sendQueue) {
        success[1] = true;
        synchronized (ConnectionTest.this) {
          ConnectionTest.this.notify();
        }
        return true;
      }

      @Override
      public void onTransportAvailable(Connection connection, String appName,
                                       String sessionID) {
        connection.handshake(appName, new ManualHandler() {
          @Override
          public void handle(JSObject message) {
            success[0] = true;
          }
        });
      }
    }));

    doAnswer(new HandshakeAnswer(connection, TestConstants.MOCK_HANDSHAKE_RESPONSE, false))
//        .when(transport).send(TestConstants.MOCK_HANDSHAKE_REQUEST);
        .when(transport).send(anyString());
    connection.connect("appName");

    synchronized (ConnectionTest.this) {
      ConnectionTest.this.wait(2000);
    }

    assertTrue(success[0] && !success[1],
        "Without session restoration 'restore' must not be called");
    success[0] = false;
    connection.onConnectionClosed();

    doAnswer(new HandshakeAnswer(connection, TestConstants.MOCK_HANDSHAKE_RESTORE, false))
        .when(transport).send(anyString());
    connection.onConnected();

    synchronized (ConnectionTest.this) {
      ConnectionTest.this.wait(2000);
    }

    assertTrue(success[0] && success[1],
        "With session restoration both methods of Restoration policy must be called");
  }

  @Test
  public void handshakeSendRecv() throws Exception {
    AbstractSocket socket = mock(AbstractSocket.class);
    when(socket.isConnected()).thenReturn(true);

    Connection connection = spy(new Connection(socket));
    doAnswer(new HandshakeAnswer(connection)).when(connection)
        .handshake(anyString(), Mockito.<ManualHandler>isNull());

    connection.handshake(TestConstants.MOCK_APP_NAME, null);

    assertTrue(connection.isConnected());
  }

  @Test
  public void handshakeError() throws Exception {
    AbstractSocket socket = mock(AbstractSocket.class);
    when(socket.isConnected()).thenReturn(true);

    final Connection connection = spy(new Connection(socket));
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        JSObject errMessage =
            JSParser.parse(TestConstants.MOCK_HANDSHAKE_RESPONSE_ERR);
        connection.onMessageReceived(errMessage);
        return null;
      }
    }).when(connection).handshake(anyString(), Mockito.<ManualHandler>isNull());

    ConnectionListener listener = mock(ConnectionListener.class);
    connection.addSocketListener(listener);

    connection.handshake(TestConstants.MOCK_APP_NAME, null);

    verify(listener, times(1))
        .onConnectionError(TestConstants.MOCK_HANDSHAKE_RESPONSE_ERR_CODE);
    assertTrue(connection.getSessionID() == null);
    assertFalse(connection.isConnected());
  }

  @Test
  void handshakeWithSession() {
    String appName = "testApp";
    String sessionId = "sessionId";

    AbstractSocket transport = mock(AbstractSocket.class);
    Connection connection = spy(new Connection(transport));
    when(transport.isConnected()).thenReturn(true);

    connection.handshake(appName, sessionId, null);

    String request = String.format(
        TestConstants.MOCK_HANDSHAKE_REQUEST_SESSION + Connection.TERMINATOR,
        appName, sessionId, 0);
    verify(transport, times(1))
        .send(request);
  }

  @Test
  void connectNullApp() {
    Throwable exception = assertThrows(RuntimeException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        AbstractSocket abstractSocket = mock(AbstractSocket.class);
        Connection connection = new Connection(abstractSocket);
        connection.connect(null);
      }
    });
    assertEquals(exception.getMessage(), "Application name must not be null");
  }

  @Test
  void handshakeNullApp() {
    Throwable exception = assertThrows(RuntimeException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        AbstractSocket abstractSocket = mock(AbstractSocket.class);
        Connection connection = new Connection(abstractSocket);
        connection.handshake(null, null);
      }
    });
    assertEquals(exception.getMessage(), "Application name must not be null");
  }

  @Test
  void checkCall() throws Exception {
    for (BasicArguments ba : callMessages) {
      String args = JSSerializer.stringify(ba.args);
      String callString = String.format(TestConstants.MOCK_CALL,
          0, ba.interfaceName, ba.methodName, args);

      connection.call(ba.interfaceName, ba.methodName, ba.args, null);

      verify(transport, times(1))
          .send(argThat(new MessageMatcherNoCount(callString)));
    }
  }

  @Test
  void checkCallHandling() throws Exception {
    ManualHandler handler = spy(ManualHandler.class);
    for (BasicArguments ba : callMessages) {
      String args = JSSerializer.stringify(ba.args);
      String callString = String.format(TestConstants.MOCK_CALL,
          0, ba.interfaceName, ba.methodName, args);
      final JSObject message = JSParser.parse(callString);

      connection.setCallHandler(ba.interfaceName, ba.methodName, handler);
      connection.onMessageReceived(message);
      connection.removeCallHandler(ba.interfaceName, ba.methodName);

      verify(handler, times(1)).handle(message);
    }
  }

  @Test
  void checkEvent() throws Exception {
    for (BasicArguments ba : eventMessages) {
      String args = JSSerializer.stringify(ba.args);
      String eventString = String.format(TestConstants.MOCK_EVENT,
          0, ba.interfaceName, ba.methodName, args);

      connection.event(ba.interfaceName, ba.methodName, ba.args);

      verify(transport, times(1))
          .send(argThat(new MessageMatcherNoCount(eventString)));
    }
  }

  @Test
  void checkEventHandling() throws Exception {
    ManualHandler handler = spy(ManualHandler.class);
    for (BasicArguments ba : eventMessages) {
      String args = JSSerializer.stringify(ba.args);
      String callString = String.format(TestConstants.MOCK_EVENT,
          0, ba.interfaceName, ba.methodName, args);
      final JSObject message = JSParser.parse(callString);

      connection.addEventHandler(ba.interfaceName, ba.methodName, handler);
      connection.onMessageReceived(message);
      connection.removeEventHandler(ba.interfaceName, ba.methodName, handler);

      verify(handler, times(1)).handle(message);
    }
  }

  @Test
  void checkCallback() throws Exception {
    for (CallbackArguments cba : callbackMessages) {
      String args = JSSerializer.stringify(cba.args);
      String callbackString = String.format(TestConstants.MOCK_CALLBACK,
          cba.messageNumber, cba.callback, args);

      connection.callback(cba.callback, cba.args, cba.messageNumber);

      verify(transport, times(1))
          .send(argThat(new MessageMatcher(callbackString)));
    }
  }

  @Test
  void checkCallbackHandling() throws Exception {
    ManualHandler handler = spy(ManualHandler.class);
    for (CallbackArguments cba : callbackMessages) {
      String args = JSSerializer.stringify(cba.args);
      String callString = String.format(TestConstants.MOCK_CALLBACK,
          cba.messageNumber, cba.callback, args);
      final JSObject message = JSParser.parse(callString);

      connection.addHandler(cba.messageNumber, handler);
      connection.onMessageReceived(message);

      verify(handler, times(1)).handle(message);
    }
  }

  @Test
  public void checkInspect() throws Exception {
    for (InspectArguments ia : inspectMessages) {

      connection.inspect(ia.interfaceName, null);

      String inspectRequest = String.format(TestConstants.MOCK_INSPECT,
          ia.messageNumber, ia.interfaceName);
      verify(transport, times(1))
          .send(argThat(new MessageMatcherNoCount(inspectRequest)));
    }
  }

  @Test
  public void checkInspectHandler() throws Exception {
    for (InspectArguments ia : inspectMessages) {
      if (ia.methods != null) {
        connection.setClientMethodNames(ia.interfaceName, ia.methods);
      }
      String inspectRequest = String.format(TestConstants.MOCK_INSPECT,
          ia.messageNumber, ia.interfaceName);

      connection.onMessageReceived(JSParser.<JSObject>parse(inspectRequest));

      JSCallback callback = JSCallback.OK;
      List<?> args = ia.methods;
      if (args == null) {
        callback = JSCallback.ERROR;
        args = Collections.singletonList(Constants.ERR_INTERFACE_NOT_FOUND);
      }
      String response = String.format(TestConstants.MOCK_CALLBACK,
          ia.messageNumber, callback, JSSerializer.stringify(args));
      verify(transport, times(1))
          .send(argThat(new MessageMatcher(response)));
    }
  }

  @Test
  public void checkHeartbeat() throws Exception {
    JSObject heartbeat = new IndexedHashMap();
    ConnectionListener listener = mock(ConnectionListener.class);

    connection.onMessageReceived(heartbeat);

    // must not reject heartbeat packet
    verify(listener, times(0))
        .onMessageRejected(ArgumentMatchers.<JSObject>notNull());
  }

  @Test
  void useTransportNotConnected() {
    AbstractSocket transport = mock(AbstractSocket.class);
    Connection connection = spy(new Connection(transport));
    when(transport.isConnected()).thenReturn(false);
    connection.connect("testApp");

    AbstractSocket anotherTransport = mock(AbstractSocket.class);
    when(anotherTransport.isConnected()).thenReturn(false);
    connection.useTransport(anotherTransport);

    verify(anotherTransport, times(1)).connect();
  }

  @Test
  void useTransportConnected() {
    String appName = "testApp";

    AbstractSocket transport = mock(AbstractSocket.class);
    Connection connection = spy(new Connection(transport));
    when(transport.isConnected()).thenReturn(false);
    // to set app name
    connection.connect(appName);

    AbstractSocket anotherSocket = mock(AbstractSocket.class);
    when(anotherSocket.isConnected()).thenReturn(true);

    connection.useTransport(anotherSocket);

    String request =
        String.format(TestConstants.MOCK_HANDSHAKE_REQUEST + Connection.TERMINATOR, appName);
    verify(anotherSocket, times(1))
        .send(request);
  }

  @Test
  void removeCallHandler() throws Exception {
    final String interfaceName = "iface";
    final String methodName = "method";

    final ManualHandler handler = spy(ManualHandler.class);
    connection.setCallHandler(interfaceName, methodName, handler);
    connection.removeCallHandler(interfaceName, methodName);

    String callString =
        String.format(TestConstants.MOCK_CALL, 13, interfaceName, methodName, "[]");
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
        String.format(TestConstants.MOCK_EVENT, 13, interfaceName, methodName, "[]");
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
}
