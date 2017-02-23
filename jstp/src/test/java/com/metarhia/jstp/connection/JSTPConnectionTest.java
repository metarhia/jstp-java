package com.metarhia.jstp.connection;

import static org.junit.Assert.assertTrue;
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
import com.metarhia.jstp.core.JSTypes.JSValue;
import com.metarhia.jstp.transport.TCPTransport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.Spy;

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
    connection.close();
    connection = null;
  }

  @Test
  public void tlsConnection() throws Exception {
    final boolean[] valid = {false};

    TCPTransport transport = new TCPTransport(TestConstants.REMOTE_HOST, TestConstants.REMOTE_PORT, true);
    JSTPConnection connection = new JSTPConnection(transport);
    connection.addSocketListener(new SimpleJSTPConnectionListener() {
      @Override
      public void onConnected(boolean restored) {
        valid[0] = true;
        synchronized (JSTPConnectionTest.this) {
          JSTPConnectionTest.this.notify();
        }
      }
    });
    connection.connect("superIn");

    synchronized (this) {
      wait(3000);
    }

    assertTrue(valid[0]);
  }

  @Test
  public void temporary() throws Exception {
    final boolean[] test = {false};
    TCPTransport transport = new TCPTransport("since.tv", 4000, true);
    final JSTPConnection connection = new JSTPConnection(transport);
    connection.addSocketListener(new SimpleJSTPConnectionListener() {
      @Override
      public void onConnected(boolean restored) {
        connection
            .call("auth", "authorize", new JSArray(new Object[]{"+380962415331", "hellokitty1337"}),
                new ManualHandler() {
                  @Override
                  public void invoke(JSValue packet) {
                    connection.call("profile", "get", new JSArray(), new ManualHandler() {
                      @Override
                      public void invoke(JSValue packet) {
                        test[0] = true;
                        synchronized (connection) {
                          connection.notify();
                        }
                      }
                    });
                  }
                });
      }
    });
    connection.connect("superIn");

    synchronized (connection) {
      connection.wait(4000);
    }

    assertTrue(test[0]);
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
    connection.addCallHandler("newAccount", new ManualHandler() {
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
  public void sendEscapedCharacters() throws Exception {
    final boolean[] test = {false};
    TCPTransport transport = new TCPTransport(TestConstants.REMOTE_HOST, TestConstants.REMOTE_PORT, true);
    final JSTPConnection connection = new JSTPConnection(transport);
    connection.addSocketListener(new SimpleJSTPConnectionListener() {
      @Override
      public void onConnected(boolean restored) {
        connection
            .call("auth", "authorize", new JSArray(new Object[]{"+380962415331", "hellokitty1337"}),
                new ManualHandler() {
                  @Override
                  public void invoke(JSValue packet) {
                    JSObject userData = new JSObject();
                    userData.put("nickname", "\n\tnyaaaaaa'aaa'[((:’ –( :-)) :-| :~ =:O)],");
                    connection.call("profile", "update", new JSArray(new Object[]{userData}),
                        new ManualHandler() {
                          @Override
                          public void invoke(JSValue packet) {
                            synchronized (connection) {
                              test[0] = true;
                              connection.notify();
                            }
                          }
                        });
                  }
                });
      }
    });
    connection.connect("superIn");

    synchronized (connection) {
      connection.wait(3000);
    }

    assertTrue(test[0]);
  }

  @Test
  public void checkPingPong() throws Exception {
    String input = "{ping:[42]}" + JSTPConnection.TERMINATOR;
    String response = "{pong:[42]}" + JSTPConnection.TERMINATOR;

    connection.onPacketReceived((JSObject) JSParser.parse(input));

    verify(transport, times(1)).send(response);
  }
}