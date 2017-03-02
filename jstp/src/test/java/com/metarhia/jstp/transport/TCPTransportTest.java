package com.metarhia.jstp.transport;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import com.metarhia.jstp.TestConstants;
import com.metarhia.jstp.connection.HandshakeAnswer;
import com.metarhia.jstp.connection.JSTPConnection;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSTypes.JSValue;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.Spy;

/**
 * Created by lundibundi on 2/18/17.
 */
public class TCPTransportTest {

  private TCPTransport tcpTransport;

  @Spy
  private JSTPConnection connection;

  @Before
  public void setUp() {
    tcpTransport = new TCPTransport("", 0);
    connection = spy(new JSTPConnection(tcpTransport));
    doAnswer(new HandshakeAnswer(connection)).when(connection)
        .handshake(anyString(), isA(ManualHandler.class));
    doAnswer(new HandshakeAnswer(connection)).when(connection)
        .handshake(anyString(), Mockito.<ManualHandler>isNull());
    connection.handshake("", null);
    // no idea why but transport has another instance of connection as listener so set this one
    tcpTransport.setSocketListener(connection);
  }

  @Test
  public void onMessageReceivedMultiple() throws Exception {
    String packet = "{callback:[17],ok:[15703]}" + JSTPConnection.TERMINATOR
        + "{event:[18,'auth'],insert:['Marcus Aurelius','AE127095']}" + JSTPConnection.TERMINATOR;

    final Boolean[] success = {false, false};

    final Thread readThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          while (tcpTransport != null) {
            tcpTransport.processMessage();
          }
          synchronized (TCPTransportTest.this) {
            TCPTransportTest.this.notify();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });

    connection.addEventHandler("auth", "insert", new ManualHandler() {
      @Override
      public void invoke(JSValue packet) {
        success[0] = true;
        synchronized (readThread) {
          if (success[1]) {
            tcpTransport = null;
            readThread.interrupt();
          }
        }
      }
    });

    connection.addHandler(17, new ManualHandler() {
      @Override
      public void invoke(JSValue packet) {
        success[1] = true;
        synchronized (readThread) {
          if (success[0]) {
            tcpTransport = null;
            readThread.interrupt();
          }
        }
      }
    });

    Field inputField = TCPTransport.class.getDeclaredField("in");
    inputField.setAccessible(true);
    final byte[] packetBytes = packet.getBytes(TestConstants.UTF_8_CHARSET);
    final ByteArrayInputStream mockStream = new ByteArrayInputStream(packetBytes);
    BufferedInputStream in = new BufferedInputStream(mockStream);
    inputField.set(tcpTransport, in);

    readThread.start();

    synchronized (TCPTransportTest.this) {
      wait(3000);
      readThread.interrupt();
    }

    assertTrue(success[0] && success[1]);
  }
}