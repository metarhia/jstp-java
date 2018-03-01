package com.metarhia.jstp.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.metarhia.jstp.TestConstants;
import com.metarhia.jstp.TestUtils;
import com.metarhia.jstp.TestUtils.ConnectionSpy;
import com.metarhia.jstp.connection.Connection;
import com.metarhia.jstp.connection.HandshakeAnswer;
import com.metarhia.jstp.connection.JSCallback;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSSerializer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CallHandlerTest {

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
  public void callHandler() throws Exception {
    final long messageNumber = 42;
    final String methodName = "method";
    String interfaceName = "interfaceName";
    final List<?> recvArgs = Collections.singletonList(null);
    final List<?> responseArgs = Arrays.asList(24, "whatever");

    final String input = String.format(TestConstants.TEMPLATE_CALL,
        messageNumber, interfaceName, methodName, JSSerializer.stringify(recvArgs));
    JSObject call = JSParser.parse(input);
    connection.setCallHandler(interfaceName, methodName, new CallHandler() {
      @Override
      public void handleCall(String method, List<?> data) {
        assertEquals(methodName, method, "Method name must be the same");
        assertEquals(data, recvArgs, "Method arguments must be the same");
        callback(connection, JSCallback.OK, responseArgs);
      }
    });

    connection.onMessageParsed(call);
    verify(connection, times(1))
        .callback(JSCallback.OK, responseArgs, messageNumber);
  }

}
