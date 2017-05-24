package com.metarhia.jstp.connection;

import com.metarhia.jstp.TestConstants;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Created by lundibundi on 2/22/17.
 */
public class HandshakeAnswer implements Answer<Void> {

  private JSTPConnection connection;
  private String response;
  private boolean manualCall;

  public HandshakeAnswer(JSTPConnection connection) {
    this(connection, "sessionId");
  }

  public HandshakeAnswer(JSTPConnection connection, String sessionId) {
    this(connection, String.format(TestConstants.MOCK_HANDSHAKE_RESPONSE, sessionId), true);
  }

  public HandshakeAnswer(JSTPConnection connection, String response, boolean manualCall) {
    this.connection = connection;
    this.response = response;
    this.manualCall = manualCall;
  }

  @Override
  public Void answer(InvocationOnMock invocation) throws Throwable {
    final JSObject handshakePacket = new JSParser(response).parse();
    connection.onPacketReceived(handshakePacket);
    if (manualCall) {
      final ManualHandler handler = invocation.getArgument(1);
      if (handler != null) {
        handler.invoke(handshakePacket);
      }
    }
    return null;
  }
}
