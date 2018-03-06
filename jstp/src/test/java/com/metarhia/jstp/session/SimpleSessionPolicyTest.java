package com.metarhia.jstp.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

class SimpleSessionPolicyTest {

  @Test
  public void restore() throws Exception {
    int numSentCalls = 1;
    int numReceivedMessages = 0;
    int expectedMessageCounter = numSentCalls + 1;
    String appName = "appName";
    String sessionId = "SessionId";
    final List<Object> callArgs = Arrays.<Object>asList(24);
    final List<Object> callbackArgs = Arrays.<Object>asList(42.0);

    Transport transport = mock(Transport.class);
    when(transport.isConnected()).thenReturn(true);
    final Connection connection = spy(new Connection(
        transport, new SimpleSessionPolicy()));
    connection.getSessionPolicy().setConnection(connection);

    doAnswer(new CallbackAnswer(connection, JSCallback.OK, callbackArgs)).when(transport)
        .send(matches(TestConstants.ANY_CALL));

    doAnswer(new HandshakeAnswer(connection, numReceivedMessages)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_RESTORE_REQUEST));

    doAnswer(new HandshakeAnswer(connection, sessionId)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_REQUEST));
    connection.connect(appName);

    assertTrue(connection.isConnected(), "Must be connected after initial mock handshake");

    // disconnect transport
    TestUtils.simulateDisconnect(connection, transport);

    OkErrorHandler handler = mock(OkErrorHandler.class);
    doCallRealMethod().when(handler)
        .onMessage(isA(JSObject.class));

    // make calls that should be repeated after connection is restored
    for (int i = 0; i < numSentCalls; ++i) {
      connection.call("interface", "method", callArgs, handler);
    }

    // connect transport
    TestUtils.simulateConnect(connection, transport);

    verify(handler, times(numSentCalls - numReceivedMessages))
        .handleOk(callbackArgs);
    verify(handler, never())
        .handleError(anyInt(), anyList());

    assertEquals(expectedMessageCounter, connection.getMessageNumberCounter(),
        "Must have correct message number");
    assertEquals(sessionId, connection.getSessionId());
  }

  @Test
  public void saveRestoreSession() throws Exception {
    File currFile =
        Paths.get("", "out", "test", "testSession2").toAbsolutePath().toFile();
    currFile.mkdirs();

    FileStorage storage = new FileStorage(currFile.getAbsolutePath());

    SimpleSessionPolicy sessionPolicy = new SimpleSessionPolicy();
    sessionPolicy.setSessionData(new SessionData("appName", "sessionId", 2, 3));
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

  @Test
  void onNewConnection() {
    SessionPolicy sessionPolicy = new SimpleSessionPolicy();

    sessionPolicy.getSessionData().setParameters("app", "session");
    sessionPolicy.getSessionData().setNumSentMessages(13);
    sessionPolicy.getSessionData().setNumReceivedMessages(42);

    SessionPolicy expectedSessionPolicy = new SimpleSessionPolicy();
    expectedSessionPolicy.getSessionData().setParameters(
        "anotherApp", "anotherSessionId");

    sessionPolicy.onNewConnection(expectedSessionPolicy.getSessionData().getAppData(),
        expectedSessionPolicy.getSessionData().getSessionId(),
        new HashMap<Long, ManualHandler>());

    assertEquals(expectedSessionPolicy, sessionPolicy,
        "must have correct values after onNewConnection");
  }

  @Test
  void orderedMessageResend() {
    SessionPolicy sessionPolicy = new SimpleSessionPolicy();
    Connection connection = mock(Connection.class);
    sessionPolicy.setConnection(connection);

    Message[] messages = {
        new Message(1, MessageType.EVENT),
        new Message(2, MessageType.EVENT),
        new Message(3, MessageType.EVENT)
    };
    for (Message message : messages) {
      sessionPolicy.onMessageSent(message);
    }

    sessionPolicy.restore(0);

    InOrder orderVerifier = Mockito.inOrder(connection);
    orderVerifier.verify(connection).send(messages[0].getStringRepresentation());
    orderVerifier.verify(connection).send(messages[1].getStringRepresentation());
    orderVerifier.verify(connection).send(messages[2].getStringRepresentation());
    orderVerifier.verifyNoMoreInteractions();
  }
}
