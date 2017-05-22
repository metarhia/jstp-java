package com.metarhia.jstp.handlers;

import com.metarhia.jstp.connection.JSCallback;
import com.metarhia.jstp.connection.JSTPConnection;
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
  public void invoke(JSObject packet) {
    callbackNumber = JSTypesUtil.<Double>getMixed(packet, 0.0, 0).longValue();
    handleCallback((List<?>) packet.getByIndex(1));
  }

  public abstract void handleCallback(List<?> data);

  public void callback(JSTPConnection connection, JSCallback result, List<?> args) {
    connection.callback(result, args, callbackNumber);
  }
}
