package com.metarhia.jstp.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.metarhia.jstp.TestConstants;
import com.metarhia.jstp.TestUtils;
import com.metarhia.jstp.TestUtils.ConnectionSpy;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.handlers.OkErrorHandler;
import com.metarhia.jstp.transport.Transport;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ResendableCallTest {

  private Connection connection;

  private Transport transport;

  private OkErrorHandler handler;

  private String appName;

  private String sessionId;

  public ResendableCallTest() {
    appName = "appName";
    sessionId = "SessionId";
  }

  @BeforeEach
  public void setUp() {
    ConnectionSpy cs = TestUtils.createConnectionSpy();
    connection = cs.connection;
    transport = cs.transport;

    doAnswer(new HandshakeAnswer(cs.connection, sessionId)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_REQUEST));

    handler = mock(OkErrorHandler.class);
    doCallRealMethod().when(handler)
        .onMessage(isA(JSObject.class));
    doCallRealMethod().when(handler)
        .onError(anyInt());

    connection.connect(appName);

    assertTrue(connection.isConnected(), "Must be connected after initial mock handshake");
  }

  @AfterEach
  public void tearDown() {
    connection.close();
  }

  @Test
  void fromDisconnected() throws Exception {
    int numReceivedMessages = 1;
    // 1 call + 1 resent call + next
    int expectedMessageCounter = 1 + 1 + 1;

    doAnswer(new CallbackAnswer(connection, JSCallback.OK, Collections.EMPTY_LIST))
        .when(transport)
        .send(matches(TestConstants.ANY_CALL));

    doAnswer(new HandshakeAnswer(connection, numReceivedMessages)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_RESTORE_REQUEST));

    // disconnect transport
    TestUtils.simulateDisconnect(connection, transport);

    connection.callResendable(
        "interface", "method", Collections.EMPTY_LIST, handler);

    // connect transport
    TestUtils.simulateConnect(connection, transport);

    verify(handler, times(1))
        .handleOk(Collections.EMPTY_LIST);
    verify(handler, never())
        .handleError(anyInt(), anyList());

    assertEquals(expectedMessageCounter, connection.getMessageNumberCounter(),
        "Must have correct message number");
  }

  @Test
  void sessionChanged() {
    // must not resend messages upon session change

    doNothing()
        .doAnswer(new CallbackAnswer(connection, JSCallback.OK, Collections.EMPTY_LIST))
        .when(transport)
        .send(matches(TestConstants.ANY_CALL));

    connection.callResendable(
        "interface", "method", Collections.EMPTY_LIST, handler);

    // disconnect transport
    TestUtils.simulateDisconnect(connection, transport);

    // reset session to simulate session failure
    connection.getSessionPolicy().getSessionData().setSessionId(null);
    // connect transport
    TestUtils.simulateConnect(connection, transport);

    verify(handler, never())
        .handleOk(anyList());
    verify(handler, never())
        .handleError(anyInt(), anyList());

    assertEquals(1, connection.getMessageNumberCounter(),
        "Must have correct message number");
  }

  @Test
  void errorPropagation() throws Exception {
    int errorCode = 113;
    int numReceivedMessages = 1;
    // 1 call + 1 resent call + next
    int expectedMessageCounter = 1 + 1 + 1;

    doAnswer(new CallbackAnswer(connection, JSCallback.ERROR, Collections.singletonList(errorCode)))
        .when(transport)
        .send(matches(TestConstants.ANY_CALL));

    doAnswer(new HandshakeAnswer(connection, numReceivedMessages)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_RESTORE_REQUEST));

    // disconnect transport
    TestUtils.simulateDisconnect(connection, transport);

    connection.callResendable(
        "interface", "method", Collections.EMPTY_LIST, handler);

    // connect transport
    TestUtils.simulateConnect(connection, transport);

    verify(handler, never())
        .handleOk(anyList());
    verify(handler, times(1))
        .handleError(eq(errorCode), eq(Collections.EMPTY_LIST));

    assertEquals(expectedMessageCounter, connection.getMessageNumberCounter(),
        "Must have correct message number");
  }

  @Test
  void noDuplicateCalls() throws Exception {
    int numReceivedMessages = 1;
    // 1 call + next
    int expectedMessageCounter = 1 + 1;

    doAnswer(new CallbackAnswer(connection, JSCallback.OK, Collections.EMPTY_LIST))
        .when(transport)
        .send(matches(TestConstants.ANY_CALL));

    doAnswer(new HandshakeAnswer(connection, numReceivedMessages)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_RESTORE_REQUEST));

    connection.callResendable(
        "interface", "method", Collections.EMPTY_LIST, handler);

    // disconnect transport
    TestUtils.simulateDisconnect(connection, transport);

    // connect transport
    TestUtils.simulateConnect(connection, transport);

    verify(handler, times(1))
        .handleOk(Collections.EMPTY_LIST);
    verify(handler, never())
        .handleError(anyInt(), anyList());

    assertEquals(expectedMessageCounter, connection.getMessageNumberCounter(),
        "Must have correct message number");
  }

  @Test
  void sentNotReceived() throws Throwable {
    int numReceivedMessages = 0;
    // 1 call + same resend call + next
    int expectedMessageCounter = 2;

    doAnswer(new HandshakeAnswer(connection, numReceivedMessages)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_RESTORE_REQUEST));

    Answer answer = mock(Answer.class);
    // first ignore then answer
    doAnswer(answer)
        .doAnswer(new CallbackAnswer(connection, JSCallback.OK, Collections.EMPTY_LIST))
        .doNothing()
        .when(transport)
        .send(matches(TestConstants.ANY_CALL));

    connection.callResendable(
        "interface", "method", Collections.EMPTY_LIST, handler);

    // disconnect transport
    TestUtils.simulateDisconnect(connection, transport);

    // connect transport
    TestUtils.simulateConnect(connection, transport);

    // must send the message via transport but it should not be received -> ignore first call
    // with stub
    verify(answer, times(1))
        .answer(any(InvocationOnMock.class));

    verify(handler, times(1))
        .handleOk(Collections.EMPTY_LIST);
    verify(handler, never())
        .handleError(anyInt(), anyList());

    assertEquals(expectedMessageCounter, connection.getMessageNumberCounter(),
        "Must have correct message number");
  }

  @Test
  void sentNoCallback() throws Throwable {
    int numReceivedMessages = 1;
    // 1 call + 1 resend call + next
    int expectedMessageCounter = 3;

    doAnswer(new HandshakeAnswer(connection, numReceivedMessages)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_RESTORE_REQUEST));

    Answer answer = mock(Answer.class);
    // first ignore then answer
    doAnswer(answer)
        .doAnswer(new CallbackAnswer(connection, JSCallback.OK, Collections.EMPTY_LIST))
        .doNothing()
        .when(transport)
        .send(matches(TestConstants.ANY_CALL));

    connection.callResendable(
        "interface", "method", Collections.EMPTY_LIST, handler);

    // disconnect transport
    TestUtils.simulateDisconnect(connection, transport);

    // connect transport
    TestUtils.simulateConnect(connection, transport);

    // must send the message via transport but it should not be received -> ignore first call
    // with stub
    verify(answer, times(1))
        .answer(any(InvocationOnMock.class));

    verify(handler, times(1))
        .handleOk(Collections.EMPTY_LIST);
    verify(handler, never())
        .handleError(anyInt(), anyList());

    assertEquals(expectedMessageCounter, connection.getMessageNumberCounter(),
        "Must have correct message number");
  }

  @Test
  void sentNoCallbackWithPing() throws Throwable {
    int numReceivedMessages = 2;
    // 1 call + 1 ping + 1 resend call + next
    int expectedMessageCounter = 4;

    doAnswer(new HandshakeAnswer(connection, numReceivedMessages)).when(transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_RESTORE_REQUEST));

    Answer loopbackAnswer = new LoopbackAnswer(connection);
    doAnswer(loopbackAnswer)
        .when(transport).send(matches(TestConstants.ANY_PING));
    doAnswer(loopbackAnswer)
        .when(transport).send(matches(TestConstants.ANY_PONG));

    Answer answer = mock(Answer.class);
    // first ignore then answer
    doAnswer(answer)
        .doAnswer(new CallbackAnswer(connection, JSCallback.OK, Collections.EMPTY_LIST))
        .doNothing()
        .when(transport)
        .send(matches(TestConstants.ANY_CALL));

    connection.callResendable(
        "interface", "method", Collections.EMPTY_LIST, handler);

    connection.ping(null);

    // disconnect transport
    TestUtils.simulateDisconnect(connection, transport);

    // connect transport
    TestUtils.simulateConnect(connection, transport);

    // must send the message via transport but it should not be received -> ignore first call
    // with stub
    verify(answer, times(1))
        .answer(any(InvocationOnMock.class));

    verify(handler, times(1))
        .handleOk(Collections.EMPTY_LIST);
    verify(handler, never())
        .handleError(anyInt(), anyList());

    assertEquals(expectedMessageCounter, connection.getMessageNumberCounter(),
        "Must have correct message number");
  }
}
