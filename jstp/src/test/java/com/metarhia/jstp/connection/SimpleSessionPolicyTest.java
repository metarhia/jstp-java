package com.metarhia.jstp.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.metarhia.jstp.TestConstants;
import com.metarhia.jstp.handlers.CallHandler;
import com.metarhia.jstp.storage.FileStorage;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimpleSessionPolicyTest {

  @Test
  public void restore() throws Exception {
    int numSentCalls = 1;
    int numReceivedMessages = 0;
    int expectedMessageCounter = numSentCalls + 1;
    String appName = "appName";
    String sessionId = "SessionId";
    final List<Object> callArgs = Arrays.<Object>asList(24.0);
    final List<Object> callbackArgs = Arrays.<Object>asList(42.0);

    AbstractSocket transport = mock(AbstractSocket.class);
    when(transport.isConnected()).thenReturn(true);
    final Connection connection = spy(new Connection(transport, new SimpleSessionPolicy()));

    doAnswer(new CallbackAnswer(connection, JSCallback.OK, callbackArgs)).when(transport)
        .send(matches(TestConstants.ANY_CALL + Connection.TERMINATOR));

    doAnswer(new HandshakeAnswer(connection, numReceivedMessages)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_RESTORE_REQUEST + Connection.TERMINATOR));

    doAnswer(new HandshakeAnswer(connection, sessionId)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_REQUEST + Connection.TERMINATOR));
    connection.connect(appName);

    assertTrue(connection.isConnected(), "Must be connected after initial mock handshake");

    // disconnect transport
    when(transport.isConnected()).thenReturn(false);
    connection.onConnectionClosed();

    // make call that should be repeated after connection is restored
    CallHandler callHandler = spy(new CallHandler() {
      @Override
      public void handleCallback(List<?> data) {
      }
    });

    for (int i = 0; i < numSentCalls; ++i) {
      connection.call("interface", "method", callArgs, callHandler);
    }

    // connect transport
    when(transport.isConnected()).thenReturn(true);
    connection.onConnected();

    verify(callHandler, times(numSentCalls - numReceivedMessages)).handleCallback(callbackArgs);
    assertEquals(expectedMessageCounter, connection.getMessageNumberCounter(),
        "Must have correct message number");
    assertEquals(sessionId, connection.getSessionId());
  }

  @Test
  void reset() {
    String newAppName = "newAppName";
    SessionData expectedSessionData = new SessionData(
        "appName", "sessionId", 2, 2);

    SessionPolicy sessionPolicy = new SimpleSessionPolicy();
    sessionPolicy.getSessionData().setAppName(expectedSessionData.getAppName());
    sessionPolicy.getSessionData().setSessionId(expectedSessionData.getSessionId());
    sessionPolicy.getSessionData().setNumReceivedMessages(2);
    sessionPolicy.getSessionData().setNumSentMessages(2);

    sessionPolicy.reset(null);
    SessionData resettedSessionData = new SessionData(expectedSessionData.getAppName(),
        expectedSessionData.getSessionId(), 0, 0);

    assertEquals(resettedSessionData, sessionPolicy.getSessionData(),
        "Must preserve app name upon reset if none was provided");

    sessionPolicy.reset(newAppName);
    SessionData fullyResettedSessionData = new SessionData(newAppName,
        expectedSessionData.getSessionId(), 0, 0);

    assertEquals(fullyResettedSessionData, sessionPolicy.getSessionData(),
        "Must set new app name upon reset if provided");
  }

  @Test
  public void saveRestoreSession() throws Exception {
    File currFile =
        Paths.get("", "out", "test", "testSession2").toAbsolutePath().toFile();
    currFile.mkdirs();

    FileStorage storage = new FileStorage(currFile.getAbsolutePath());

    SimpleSessionPolicy sessionPolicy = new SimpleSessionPolicy();
    sessionPolicy.getSessionData().setAppName("appName");
    sessionPolicy.getSessionData().setSessionId("sessionId");
    sessionPolicy.getSessionData().setNumReceivedMessages(2);
    sessionPolicy.getSessionData().setNumSentMessages(3);
    sessionPolicy.put("data", new ArrayList(Arrays.asList(1, 2, 3)));
    Message testMessage = new Message(13, MessageType.CALL);
    sessionPolicy.onMessageSent(testMessage);

    sessionPolicy.saveSession(storage);

    SimpleSessionPolicy restoredSessionPolicy = SimpleSessionPolicy.restoreFrom(storage);

    assertEquals(sessionPolicy, restoredSessionPolicy);

    for (File f : currFile.listFiles()) {
      f.delete();
    }
    currFile.delete();
  }

}