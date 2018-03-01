package com.metarhia.jstp.transport;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.metarhia.jstp.Constants;
import com.metarhia.jstp.TestConstants;
import com.metarhia.jstp.TestUtils;
import com.metarhia.jstp.connection.Connection;
import com.metarhia.jstp.connection.HandshakeAnswer;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.handlers.OkErrorHandler;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Created by lundibundi on 2/18/17.
 */
public class TCPTransportTest {

  private TCPTransport tcpTransport;

  private Connection connection;

  public TCPTransportTest() {
    tcpTransport = spy(new TCPTransport("", 0));
    connection = TestUtils.createConnectionSpy(tcpTransport, true).connection;
    doAnswer(new HandshakeAnswer(connection)).when(tcpTransport)
        .send(matches(TestConstants.ANY_HANDSHAKE_REQUEST));
    connection.connect("appName");
    assertTrue(connection.isConnected(), "Must be connected after handshake");
  }

  @Test
  public void onMessageReceivedMultiple() throws Exception {
    String callbackMessage = "{callback:[17],ok:[15703]}";
    String eventMessage = "{event:[18,'auth'],insert:['Marcus Aurelius','AE127095']}";
    String packet = callbackMessage + Constants.SEPARATOR + eventMessage + Constants.SEPARATOR;

    final byte[] packetBytes = packet.getBytes(TestConstants.UTF_8_CHARSET);
    final ByteArrayInputStream mockStream = new ByteArrayInputStream(packetBytes);
    final BufferedInputStream in = new BufferedInputStream(mockStream);
    final ByteArrayOutputStream baos = new ByteArrayOutputStream(100);

    final Thread readThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          while (in.available() > 0) {
            tcpTransport.processMessage(in, baos);
          }
        } catch (IOException e) {
          fail("Unexpected error during message feed", e);
        }
      }
    });

    ManualHandler eventHandler = mock(ManualHandler.class);
    connection.addEventHandler("auth", "insert", eventHandler);

    OkErrorHandler handler = mock(OkErrorHandler.class);
    connection.addHandler(17, handler);

    readThread.start();

    synchronized (TCPTransportTest.this) {
      wait(1000);
    }

    verify(eventHandler, times(1))
        .onMessage(JSParser.<JSObject>parse(eventMessage));
    verify(handler, times(1))
        .onMessage(JSParser.<JSObject>parse(callbackMessage));
  }
}
