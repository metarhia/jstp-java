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

  private Connection connection;
  private String response;
  private boolean manualCall;

  public HandshakeAnswer(Connection connection) {
    this(connection, "sessionId");
  }

  public HandshakeAnswer(Connection connection, String sessionId) {
    this(connection, String.format(TestConstants.MOCK_HANDSHAKE_RESPONSE, sessionId), true);
  }

  public HandshakeAnswer(Connection connection, String response, boolean manualCall) {
    this.connection = connection;
    this.response = response;
    this.manualCall = manualCall;
  }

  @Override
  public Void answer(InvocationOnMock invocation) throws Throwable {
    final JSObject handshakeMessage = new JSParser(response).parse();
    connection.onMessageReceived(handshakeMessage);
    if (manualCall) {
      final ManualHandler handler = invocation.getArgument(1);
      if (handler != null) {
        handler.handle(handshakeMessage);
      }
    }
    return null;
  }
}
