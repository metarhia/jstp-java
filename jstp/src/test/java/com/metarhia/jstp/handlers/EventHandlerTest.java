package com.metarhia.jstp.handlers;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.metarhia.jstp.TestConstants;
import com.metarhia.jstp.TestUtils;
import com.metarhia.jstp.TestUtils.ConnectionSpy;
import com.metarhia.jstp.connection.Connection;
import com.metarhia.jstp.connection.HandshakeAnswer;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSSerializer;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventHandlerTest {

  private Connection connection;

  @BeforeEach
  public void setUp() throws Exception {
    ConnectionSpy cs = TestUtils.createConnectionSpy();
    connection = cs.connection;
    doAnswer(new HandshakeAnswer(connection)).when(cs.transport)
        .send(matches(TestConstants.ANY_HANDSHAKE_REQUEST));
    connection.handshake(TestConstants.MOCK_APP_NAME, null);
  }

  @Test
  public void eventHandler() throws Exception {
    long messageNumber = 42;
    final String eventName = "method";
    String interfaceName = "interfaceName";
    final List<?> eventArgs = Arrays.asList(1, 2, 3);

    final String rawEventData = String.format(TestConstants.TEMPLATE_EVENT,
        messageNumber, interfaceName, eventName, JSSerializer.stringify(eventArgs));
    JSObject eventMessage = JSParser.parse(rawEventData);
    EventHandler eventHandler = mock(EventHandler.class);
    doCallRealMethod().when(eventHandler).onMessage(isA(JSObject.class));
    connection.addEventHandler(interfaceName, eventName, eventHandler);
    connection.onMessageParsed(eventMessage);

    verify(eventHandler, times(1))
        .handleEvent(eventName, eventArgs);
  }
}
