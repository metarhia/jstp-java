package com.metarhia.jstp.connection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.metarhia.jstp.TestConstants;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSTypes.JSArray;
import com.metarhia.jstp.core.JSTypes.JSObject;
import com.metarhia.jstp.core.JSTypes.JSString;
import com.metarhia.jstp.core.JSTypes.JSValue;
import com.metarhia.jstp.transport.TCPTransport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class JSTPConnectionTest {

  @Spy
  private JSTPConnection connection;

  private AbstractSocket transport;

  @Before
  public void setUp() {
    transport = mock(AbstractSocket.class);
    connection = spy(new JSTPConnection(transport));
    doAnswer(new HandshakeAnswer(connection)).when(connection)
        .handshake(anyString(), isA(ManualHandler.class));
    doAnswer(new HandshakeAnswer(connection)).when(connection)
        .handshake(anyString(), Mockito.<ManualHandler>isNull());
    when(transport.isConnected()).thenReturn(true);
    connection.handshake(TestConstants.MOCK_APP_NAME, null);
  }

  @After
  public void tearDown() {
    if (connection != null) {
      connection.close();
      connection = null;
    }
  }

  @Test
  public void emptyObject() throws Exception {
    final String s = "{}" + JSTPConnection.TERMINATOR;
    final boolean[] success = {false};
    connection.addSocketListener(new SimpleJSTPConnectionListener() {
      @Override
      public void onPacketRejected(JSObject packet) {
        success[0] = true;
      }
    });

    connection.onPacketReceived((JSObject) JSParser.parse(s));
    assertTrue(success[0]);
  }

  @Test
  public void onMessageReceivedCall() throws Exception {
    String packet = "{call:[17,'auth'], newAccount:['Payload data']}" + JSTPConnection.TERMINATOR;

    final Boolean[] success = {false};
    connection.setCallHandler("newAccount", new ManualHandler() {
      @Override
      public void invoke(JSValue packet) {
        success[0] = true;
      }
    });

    connection.onPacketReceived((JSObject) JSParser.parse(packet));

    assertTrue(success[0]);
  }

  @Test
  public void onMessageReceivedEvent() throws Exception {
    String packet =
        "{event:[18,'auth'],insert:['Marcus Aurelius','AE127095']}" + JSTPConnection.TERMINATOR;

    final Boolean[] success = {false};
    connection.addEventHandler("auth", "insert", new ManualHandler() {
      @Override
      public void invoke(JSValue packet) {
        success[0] = true;
      }
    });

    connection.onPacketReceived((JSObject) JSParser.parse(packet));

    assertTrue(success[0]);
  }

  @Test
  public void onMessageReceivedCallback() throws Exception {
    String packet = "{callback:[17],ok:[15703]}" + JSTPConnection.TERMINATOR;

    final Boolean[] success = {false};
    connection.addHandler(17, new ManualHandler() {
      @Override
      public void invoke(JSValue packet) {
        success[0] = true;
      }
    });

    connection.onPacketReceived((JSObject) JSParser.parse(packet));

    assertTrue(success[0]);
  }

  @Test
  public void checkPingPong() throws Exception {
    String input = "{ping:[42]}" + JSTPConnection.TERMINATOR;
    String response = "{pong:[42]}" + JSTPConnection.TERMINATOR;

    connection.onPacketReceived((JSObject) JSParser.parse(input));

    verify(transport, times(1)).send(response);
  }

  @Test
  public void checkCallback() throws Exception {
    final JSArray args = new JSArray(new Object[]{"data"});
    int packageNumber = 13;
    String message = String.format("{callback:[%d],ok:%s}" + JSTPConnection.TERMINATOR,
        packageNumber, args);

    connection.callback(JSCallback.OK, args, packageNumber);

    verify(transport, times(1)).send(message);
  }

  @Test
  public void checkInspectCall() throws Exception {
    String interfaceName = "interfaceName";
    String message = String.format("\\{inspect:\\[\\d+,'%s'\\]\\}" + JSTPConnection.TERMINATOR,
        interfaceName);

    connection.inspect(interfaceName, null);

    verify(transport, times(1)).send(matches(message));
  }

  @Test
  public void checkInspectResponse() throws Exception {
    String interfaceName = "interfaceName";
    connection.setClientMethodNames(interfaceName, "method1", "method2");
    String methods = "'method1','method2'";
    String message = String
        .format("\\{callback:\\[\\d+\\],ok:\\[%s\\]\\}" + JSTPConnection.TERMINATOR, methods);

    String inspectMessage = String.format("{inspect:[12, %s]}", new JSString(interfaceName));
    JSObject inspectPacket = new JSParser(inspectMessage).parseObject();
    connection.onPacketReceived(inspectPacket);

    verify(transport, times(1)).send(matches(message));
  }

  @Test
  public void handshakeSendRecv() throws Exception {
    AbstractSocket socket = mock(AbstractSocket.class);
    when(socket.isConnected()).thenReturn(true);

    JSTPConnection connection = spy(new JSTPConnection(socket));
    doAnswer(new HandshakeAnswer(connection)).when(connection)
        .handshake(anyString(), Mockito.<ManualHandler>isNull());

    connection.handshake(TestConstants.MOCK_APP_NAME, null);

    assertTrue(connection.isHandshakeFinished());
  }

  @Test
  public void handshakeError() throws Exception {
    AbstractSocket socket = mock(AbstractSocket.class);
    when(socket.isConnected()).thenReturn(true);

    final JSTPConnection connection = spy(new JSTPConnection(socket));
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        JSObject errPacket = new JSParser(TestConstants.MOCK_HANDSHAKE_RESPONSE_ERR)
            .parseObject();
        connection.onPacketReceived(errPacket);
        return null;
      }
    }).when(connection)
        .handshake(anyString(), Mockito.<ManualHandler>isNull());

    JSTPConnectionListener listener = mock(JSTPConnectionListener.class);
    connection.addSocketListener(listener);

    connection.handshake(TestConstants.MOCK_APP_NAME, null);

    verify(listener, times(1))
        .onConnectionError(TestConstants.MOCK_HANDSHAKE_RESPONSE_ERR_CODE);
    assertTrue(connection.getSessionID() == null);
    assertFalse(connection.isHandshakeFinished());
  }
}
