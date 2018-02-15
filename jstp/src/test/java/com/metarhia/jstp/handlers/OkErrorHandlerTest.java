package com.metarhia.jstp.handlers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.metarhia.jstp.JSTP;
import com.metarhia.jstp.TestConstants;
import com.metarhia.jstp.connection.JSCallback;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.exceptions.MessageHandlingException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class OkErrorHandlerTest {

  @Test
  public void okStatus() throws Exception {
    OkErrorHandler handler = spy(OkErrorHandler.class);

    final List<?> data = Arrays.asList(24, "whatever");

    final String input = String.format(TestConstants.TEMPLATE_CALLBACK,
        42, JSCallback.OK.toString(), JSTP.stringify(data));
    JSObject callback = JSParser.parse(input);

    handler.handle(callback);

    verify(handler, times(1))
        .handleOk(data);
    verify(handler, never())
        .handleError(isA(List.class));
  }

  @Test
  public void errorStatus() throws Exception {
    OkErrorHandler handler = spy(OkErrorHandler.class);

    final List<?> data = Arrays.asList(42, "errorCode");

    final String input = String.format(TestConstants.TEMPLATE_CALLBACK,
        42, JSCallback.ERROR.toString(), JSTP.stringify(data));
    JSObject callback = JSParser.parse(input);

    handler.handle(callback);

    verify(handler, never())
        .handleOk(isA(List.class));
    verify(handler, times(1))
        .handleError(data);
  }

  @Test
  public void invalidStatus() throws Exception {
    final OkErrorHandler handler = spy(OkErrorHandler.class);

    final List<?> data = Arrays.asList(42, "errorCode");

    final String input = String.format(TestConstants.TEMPLATE_CALLBACK,
        42, "__invalid_status__", JSTP.stringify(data));
    final JSObject callback = JSParser.parse(input);

    assertThrows(MessageHandlingException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        handler.handle(callback);
      }
    });

    verify(handler, never())
        .handleOk(isA(List.class));
    verify(handler, never())
        .handleError(isA(List.class));
  }
}
