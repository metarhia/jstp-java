package com.metarhia.jstp.transport;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import com.metarhia.jstp.TestConstants;
import com.metarhia.jstp.connection.Connection;
import com.metarhia.jstp.connection.HandshakeAnswer;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.Spy;

/**
 * Created by lundibundi on 2/18/17.
 */
public class TCPTransportTest {

  private TCPTransport tcpTransport;

  @Spy
  private Connection connection;

  public TCPTransportTest() {
    tcpTransport = new TCPTransport("", 0);
    connection = spy(new Connection(tcpTransport));
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
    String packet = "{callback:[17],ok:[15703]}" + Connection.TERMINATOR
        + "{event:[18,'auth'],insert:['Marcus Aurelius','AE127095']}" + Connection.TERMINATOR;

    final byte[] packetBytes = packet.getBytes(TestConstants.UTF_8_CHARSET);
    final ByteArrayInputStream mockStream = new ByteArrayInputStream(packetBytes);
    final BufferedInputStream in = new BufferedInputStream(mockStream);
    final ByteArrayOutputStream baos = new ByteArrayOutputStream(100);

    final Boolean[] success = {false, false};

    final Thread readThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          while (tcpTransport != null) {
            tcpTransport.processMessage(in, baos);
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
      public void handle(JSObject message) {
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
      public void handle(JSObject message) {
        success[1] = true;
        synchronized (readThread) {
          if (success[0]) {
            tcpTransport = null;
            readThread.interrupt();
          }
        }
      }
    });

    readThread.start();

    synchronized (TCPTransportTest.this) {
      wait(3000);
      readThread.interrupt();
    }

    assertTrue(success[0] && success[1]);
  }
}
