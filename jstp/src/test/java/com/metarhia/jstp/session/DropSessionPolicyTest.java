package com.metarhia.jstp.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.metarhia.jstp.TestConstants;
import com.metarhia.jstp.TestUtils;
import com.metarhia.jstp.connection.CallbackAnswer;
import com.metarhia.jstp.connection.Connection;
import com.metarhia.jstp.connection.HandshakeAnswer;
import com.metarhia.jstp.connection.JSCallback;
import com.metarhia.jstp.connection.Message;
import com.metarhia.jstp.connection.MessageType;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.handlers.OkErrorHandler;
import com.metarhia.jstp.storage.FileStorage;
import com.metarhia.jstp.transport.Transport;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class DropSessionPolicyTest {

  @Test
  public void restore() {
    String appName = "appName";
    String sessionId = "sessionId";
    String anotherSessionId = "anotherSessionId";
    long numSentCalls = 2;
    final List<Object> callArgs = Arrays.<Object>asList(24.0);
    final List<Object> callbackArgs = Arrays.<Object>asList(42.0);

    Transport transport = mock(Transport.class);
    when(transport.isConnected()).thenReturn(true);
    final Connection connection = spy(new Connection(
        transport, new DropSessionPolicy()));
    connection.getSessionPolicy().setConnection(connection);

    doAnswer(new CallbackAnswer(connection, JSCallback.OK, callbackArgs)).when(transport)
        .send(matches(TestConstants.ANY_CALL));

    doAnswer(new HandshakeAnswer(connection, sessionId)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_REQUEST));
    connection.connect(appName);

    assertTrue(connection.isConnected(), "Must be connected after initial mock handshake");

    // disconnect transport
    TestUtils.simulateDisconnect(connection, transport);

    OkErrorHandler handler = mock(OkErrorHandler.class);

    // make calls that should be repeated after connection is restored
    for (int i = 0; i < numSentCalls; ++i) {
      connection.call("interface", "method", callArgs, handler);
    }

    doAnswer(new HandshakeAnswer(connection, anotherSessionId)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_REQUEST));

    // connect transport
    TestUtils.simulateConnect(connection, transport);

    verify(handler, never())
        .onMessage(any(JSObject.class));

    assertEquals(1, connection.getMessageNumberCounter(),
        "Must have correct message number");
    assertNotEquals(sessionId, connection.getSessionId());
    assertEquals(anotherSessionId, connection.getSessionId());
  }


  @Test
  public void saveRestoreSession() throws Exception {
    File currFile =
        Paths.get("", "out", "test", "testSession2").toAbsolutePath().toFile();
    currFile.mkdirs();

    FileStorage storage = new FileStorage(currFile.getAbsolutePath());

    DropSessionPolicy sessionPolicy = new DropSessionPolicy();
    sessionPolicy.setSessionData(new SessionData("appName", "sessionId", 2, 3));
    Message testMessage = new Message(13, MessageType.CALL);
    sessionPolicy.onMessageSent(testMessage);

    sessionPolicy.saveSession(storage);

    DropSessionPolicy restoredSessionPolicy = DropSessionPolicy.restoreFrom(storage);

    assertEquals(sessionPolicy, restoredSessionPolicy);

    for (File f : currFile.listFiles()) {
      f.delete();
    }
    currFile.delete();
  }

  @Test
  void onNewConnection() {
    SessionPolicy sessionPolicy = new DropSessionPolicy();

    sessionPolicy.getSessionData().setParameters("app", "session");
    sessionPolicy.getSessionData().setNumSentMessages(13);
    sessionPolicy.getSessionData().setNumReceivedMessages(42);

    SessionPolicy expectedSessionPolicy = new DropSessionPolicy();
    expectedSessionPolicy.getSessionData().setParameters(
        "anotherApp", "anotherSessionId");

    sessionPolicy.onNewConnection(expectedSessionPolicy.getSessionData().getAppData(),
        expectedSessionPolicy.getSessionData().getSessionId(),
        new HashMap<Long, ManualHandler>());


    assertEquals(expectedSessionPolicy, sessionPolicy,
        "must have correct values after onNewConnection");
  }
}
