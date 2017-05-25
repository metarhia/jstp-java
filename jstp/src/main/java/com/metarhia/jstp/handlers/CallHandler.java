package com.metarhia.jstp.handlers;

import com.metarhia.jstp.connection.JSCallback;
import com.metarhia.jstp.connection.Connection;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSTypes.JSTypesUtil;
import java.util.List;

/**
 * Created by lundibundi on 3/12/17.
 */
public abstract class CallHandler implements ManualHandler {

  private long callbackNumber;

  @Override
  public void handle(JSObject message) {
    callbackNumber = JSTypesUtil.<Double>getMixed(message, 0.0, 0).longValue();
    handleCallback((List<?>) message.getByIndex(1));
  }

  public abstract void handleCallback(List<?> data);

  public void callback(Connection connection, JSCallback result, List<?> args) {
    connection.callback(result, args, callbackNumber);
  }
}
