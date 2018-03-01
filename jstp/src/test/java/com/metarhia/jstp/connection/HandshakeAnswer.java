package com.metarhia.jstp.connection;

import com.metarhia.jstp.TestConstants;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSParser;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Created by lundibundi on 2/22/17.
 */
public class HandshakeAnswer implements Answer<Void> {

  private Connection connection;
  private String response;

  public HandshakeAnswer(Connection connection) {
    this(connection, "sessionId");
  }

  public HandshakeAnswer(Connection connection, String sessionId) {
    this.connection = connection;
    this.response = String.format(TestConstants.TEMPLATE_HANDSHAKE_RESPONSE, sessionId);
  }

  public HandshakeAnswer(Connection connection, long numReceivedMessages) {
    this.connection = connection;
    this.response = String.format(TestConstants.TEMPLATE_HANDSHAKE_RESTORE_RESPONSE,
        numReceivedMessages);
  }

  @Override
  public Void answer(InvocationOnMock invocation) throws Throwable {
    final JSObject handshakeAnswer = JSParser.parse(response);
    connection.onMessageParsed(handshakeAnswer);
    return null;
  }
}
