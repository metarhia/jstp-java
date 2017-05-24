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
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class JSTPConnectionTest {

  private static final List<BasicArguments> callPackets = Arrays.asList(
      new BasicArguments("iface3", "method", Collections.EMPTY_LIST),
      new BasicArguments("iface2", "method2", Collections.singletonList(13)),
      new BasicArguments("iface", "method3", Arrays.asList(1, "abc", 42)));

  private static final List<CallbackArguments> callbackPackets = Arrays.asList(
      new CallbackArguments(13, JSCallback.OK, Collections.EMPTY_LIST),
      new CallbackArguments(11, JSCallback.OK, Collections.singletonList(13)),
      new CallbackArguments(42, JSCallback.OK, Arrays.asList(1, "abc", 42)),
      new CallbackArguments(43, JSCallback.ERROR, Arrays.asList(1, "Remote error")));

  private static final List<BasicArguments> eventPackets = callPackets;

  private static final List<InspectArguments> inspectPackets = Arrays.asList(
      new InspectArguments(11, "iface1",
          Arrays.asList("method")),
      new InspectArguments(11, "iface2",
          Arrays.asList("method1, method2")),
      new InspectArguments(13, "auth",
          null));

  private static final Map<String, List<String>> illPackets = new HashMap<>();

  static {
    illPackets.put("Call packet", Arrays.asList(
        "{call:['ss','auth'],newAccount:{a: 'Payload data'}}",
        "{call:[17,14],newAccount:['Payload data']}",
        "{call:[17,'auth']}"));
    illPackets.put("Callback packet", Arrays.asList(
        "{callback:['ss'],ok:[15703]}"));

    illPackets.put("Event packet", Arrays.asList(
        "{event:['chat'],message:['Marcus','Hello there!']}",
        "{event:[-12,'chat']}"));

//    illPackets.put("Handshake request packet", Arrays.asList(
//        "{handshake:[0],marcus:'7b458e1a9dda67cb7a3e'}",
//        "{handshake:[0,'example']}",
//        "{handshake:[0,'example'],marcus:1111}",
//        "{handshake:[0,'example'],marcus:'7b458e1a9dda67cb7a3e'}"
//    ));

    illPackets.put("Handshake response packet", Arrays.asList(
        "{handshake:['xx'],ok:'9b71d224bd62bcdec043'}",
        "{handshake:[0],ok:333}"));

    illPackets.put("inspect request packet", Arrays.asList(
        "{inspect:['22','interfaceName']}",
        "{inspect:[42]}"));

    illPackets.put("inspect response packet", Arrays.asList(
        "{callback:[42]}",
        "{callback:[42],ok:{'method1':'method2'}}"));
  }

  private static JSTPConnectionTest instance;

  @Spy
  private JSTPConnection connection;

  private AbstractSocket transport;

  public JSTPConnectionTest() {
    instance = this;
    transport = mock(AbstractSocket.class);
    connection = spy(new JSTPConnection(transport));
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
  public void emptyObject() throws Exception {
    final String packet = "{}" + JSTPConnection.TERMINATOR;
    final boolean[] success = {false};
    connection.addSocketListener(new SimpleJSTPConnectionListener() {
      @Override
      public void onPacketRejected(JSObject packet) {
        success[0] = true;
      }
    });

    connection.onPacketReceived(JSParser.<IndexedHashMap<?>>parse(packet));
    assertTrue(success[0]);
  }

  @Test
  public void checkPingPong() throws Exception {
    String input = "{ping:[42]}" + JSTPConnection.TERMINATOR;
    String response = "{pong:[42]}" + JSTPConnection.TERMINATOR;

    connection.onPacketReceived(JSParser.<JSObject>parse(input));

    verify(transport, times(1))
        .send(argThat(new MessageMatcher(response)));
  }

  @Test
  public void saveRestoreSession() throws Exception {
    String folder = "/tmp";
    FileStorage storage = new FileStorage(folder);

    connection.saveSession(storage);

    AbstractSocket socket = mock(AbstractSocket.class);
    JSTPConnection anotherConn = new JSTPConnection(socket);

    anotherConn.restoreSession(storage);

    assertTrue(connection.getSessionData().equals(anotherConn.getSessionData()));
  }

  @Test
  public void illFormedPackets() throws Exception {
    JSTPConnectionListener listener = spy(JSTPConnectionListener.class);
    connection.addSocketListener(listener);
    JSParser parser = new JSParser();
    for (Map.Entry<String, List<String>> illList : illPackets.entrySet()) {
      for (String messageString : illList.getValue()) {
        parser.setInput(messageString);
        final JSObject message = parser.parse();
        connection.onPacketReceived(message);

        verify(listener, times(1)).onPacketRejected(message);

        connection.handshake(TestConstants.MOCK_APP_NAME, null);
      }
    }
  }

  @Test
  public void callbackHandler() throws Exception {
    long packetNum = 42;
    String methodName = "method";
    String interfaceName = "interfaceName";
    final List<?> recvArgs = new ArrayList<>();
    recvArgs.add(null);
    final List<?> responseArgs = Arrays.asList(24, "whatever");

    final String input =
        String.format("{call:[%d,'%s'], %s:%s}", packetNum, interfaceName,
            methodName, JSSerializer.stringify(recvArgs));
    JSObject callback = JSParser.parse(input);
    connection.setCallHandler(interfaceName, "method", new CallHandler() {
      @Override
      public void handleCallback(List<?> data) {
        assertEquals(data, recvArgs);
        callback(connection, JSCallback.OK, responseArgs);
      }
    });
    connection.onPacketReceived(callback);
    verify(connection, times(1))
        .callback(JSCallback.OK, responseArgs, packetNum);
  }

  @Test
  public void restorationPolicyCheck() throws Exception {
    final boolean[] success = {false, false};

    AbstractSocket transport = mock(AbstractSocket.class);
    when(transport.isConnected()).thenReturn(true);

    JSTPConnection connection = spy(new JSTPConnection(transport, new RestorationPolicy() {
      @Override
      public boolean restore(JSTPConnection connection, Queue<JSTPMessage> sendQueue) {
        success[1] = true;
        synchronized (JSTPConnectionTest.this) {
          JSTPConnectionTest.this.notify();
        }
        return true;
      }

      @Override
      public void onTransportAvailable(JSTPConnection connection, String appName,
                                       String sessionID) {
        connection.handshake(appName, new ManualHandler() {
          @Override
          public void invoke(JSObject packet) {
            success[0] = true;
          }
        });
      }
    }));

    doAnswer(new HandshakeAnswer(connection, TestConstants.MOCK_HANDSHAKE_RESPONSE, false))
//        .when(transport).send(TestConstants.MOCK_HANDSHAKE_REQUEST);
        .when(transport).send(anyString());
    connection.connect("appName");

    synchronized (JSTPConnectionTest.this) {
      JSTPConnectionTest.this.wait(2000);
    }

    assertTrue(success[0] && !success[1],
        "Without session restoration 'restore' must not be called");
    success[0] = false;
    connection.onConnectionClosed();

    doAnswer(new HandshakeAnswer(connection, TestConstants.MOCK_HANDSHAKE_RESTORE, false))
        .when(transport).send(anyString());
    connection.onConnected();

    synchronized (JSTPConnectionTest.this) {
      JSTPConnectionTest.this.wait(2000);
    }

    assertTrue(success[0] && success[1],
        "With session restoration both methods of Restoration policy must be called");
  }

  @Test
  public void handshakeSendRecv() throws Exception {
    AbstractSocket socket = mock(AbstractSocket.class);
    when(socket.isConnected()).thenReturn(true);

    JSTPConnection connection = spy(new JSTPConnection(socket));
    doAnswer(new HandshakeAnswer(connection)).when(connection)
        .handshake(anyString(), Mockito.<ManualHandler>isNull());

    connection.handshake(TestConstants.MOCK_APP_NAME, null);

    assertTrue(connection.isConnected());
  }

  @Test
  public void handshakeError() throws Exception {
    AbstractSocket socket = mock(AbstractSocket.class);
    when(socket.isConnected()).thenReturn(true);

    final JSTPConnection connection = spy(new JSTPConnection(socket));
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        JSObject errPacket =
            JSParser.parse(TestConstants.MOCK_HANDSHAKE_RESPONSE_ERR);
        connection.onPacketReceived(errPacket);
        return null;
      }
    }).when(connection).handshake(anyString(), Mockito.<ManualHandler>isNull());

    JSTPConnectionListener listener = mock(JSTPConnectionListener.class);
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
    JSTPConnection connection = spy(new JSTPConnection(transport));
    when(transport.isConnected()).thenReturn(true);

    connection.handshake(appName, sessionId, null);

    String request = String.format(
        TestConstants.MOCK_HANDSHAKE_REQUEST_SESSION + JSTPConnection.TERMINATOR,
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
        JSTPConnection connection = new JSTPConnection(abstractSocket);
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
        JSTPConnection connection = new JSTPConnection(abstractSocket);
        connection.handshake(null, null);
      }
    });
    assertEquals(exception.getMessage(), "Application name must not be null");
  }

  @Test
  void checkCall() throws Exception {
    for (BasicArguments ba : callPackets) {
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
    for (BasicArguments ba : callPackets) {
      String args = JSSerializer.stringify(ba.args);
      String callString = String.format(TestConstants.MOCK_CALL,
          0, ba.interfaceName, ba.methodName, args);
      final JSObject message = JSParser.parse(callString);

      connection.setCallHandler(ba.interfaceName, ba.methodName, handler);
      connection.onPacketReceived(message);
      connection.removeCallHandler(ba.interfaceName, ba.methodName);

      verify(handler, times(1)).invoke(message);
    }
  }

  @Test
  void checkEvent() throws Exception {
    for (BasicArguments ba : eventPackets) {
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
    for (BasicArguments ba : eventPackets) {
      String args = JSSerializer.stringify(ba.args);
      String callString = String.format(TestConstants.MOCK_EVENT,
          0, ba.interfaceName, ba.methodName, args);
      final JSObject message = JSParser.parse(callString);

      connection.addEventHandler(ba.interfaceName, ba.methodName, handler);
      connection.onPacketReceived(message);
      connection.removeEventHandler(ba.interfaceName, ba.methodName, handler);

      verify(handler, times(1)).invoke(message);
    }
  }

  @Test
  void checkCallback() throws Exception {
    for (CallbackArguments cba : callbackPackets) {
      String args = JSSerializer.stringify(cba.args);
      String callbackString = String.format(TestConstants.MOCK_CALLBACK,
          cba.packageCounter, cba.callback, args);

      connection.callback(cba.callback, cba.args, cba.packageCounter);

      verify(transport, times(1))
          .send(argThat(new MessageMatcher(callbackString)));
    }
  }

  @Test
  void checkCallbackHandling() throws Exception {
    ManualHandler handler = spy(ManualHandler.class);
    for (CallbackArguments cba : callbackPackets) {
      String args = JSSerializer.stringify(cba.args);
      String callString = String.format(TestConstants.MOCK_CALLBACK,
          cba.packageCounter, cba.callback, args);
      final JSObject message = JSParser.parse(callString);

      connection.addHandler(cba.packageCounter, handler);
      connection.onPacketReceived(message);

      verify(handler, times(1)).invoke(message);
    }
  }

  @Test
  public void checkInspect() throws Exception {
    for (InspectArguments ia : inspectPackets) {

      connection.inspect(ia.interfaceName, null);

      String inspectRequest = String.format(TestConstants.MOCK_INSPECT,
          ia.packetCounter, ia.interfaceName);
      verify(transport, times(1))
          .send(argThat(new MessageMatcherNoCount(inspectRequest)));
    }
  }

  @Test
  public void checkInspectHandler() throws Exception {
    for (InspectArguments ia : inspectPackets) {
      if (ia.methods != null) {
        connection.setClientMethodNames(ia.interfaceName, ia.methods);
      }
      String inspectRequest = String.format(TestConstants.MOCK_INSPECT,
          ia.packetCounter, ia.interfaceName);

      connection.onPacketReceived(JSParser.<JSObject>parse(inspectRequest));

      JSCallback callback = JSCallback.OK;
      List<?> args = ia.methods;
      if (args == null) {
        callback = JSCallback.ERROR;
        args = Collections.singletonList(Constants.ERR_INTERFACE_NOT_FOUND);
      }
      String response = String.format(TestConstants.MOCK_CALLBACK,
          ia.packetCounter, callback, JSSerializer.stringify(args));
      verify(transport, times(1))
          .send(argThat(new MessageMatcher(response)));
    }
  }

  @Test
  void useTransportNotConnected() {
    AbstractSocket transport = mock(AbstractSocket.class);
    JSTPConnection connection = spy(new JSTPConnection(transport));
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
    JSTPConnection connection = spy(new JSTPConnection(transport));
    when(transport.isConnected()).thenReturn(false);
    // to set app name
    connection.connect(appName);

    AbstractSocket anotherSocket = mock(AbstractSocket.class);
    when(anotherSocket.isConnected()).thenReturn(true);

    connection.useTransport(anotherSocket);

    String request =
        String.format(TestConstants.MOCK_HANDSHAKE_REQUEST + JSTPConnection.TERMINATOR, appName);
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
    connection.onPacketReceived(JSParser.<JSObject>parse(callString));

    verify(handler, times(0)).invoke(any(JSObject.class));
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
    connection.onPacketReceived(JSParser.<JSObject>parse(callString));

    verify(handler, times(0)).invoke(any(JSObject.class));
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

    long packageCounter;
    JSCallback callback;
    List<?> args;

    public CallbackArguments(long packageCounter, JSCallback callback, List<?> args) {
      this.packageCounter = packageCounter;
      this.callback = callback;
      this.args = args;
    }
  }

  private static class InspectArguments {

    long packetCounter;
    String interfaceName;
    List<String> methods;

    public InspectArguments(long packetCounter, String interfaceName,
                            List<String> methods) {
      this.packetCounter = packetCounter;
      this.interfaceName = interfaceName;
      this.methods = methods;
    }
  }
}
