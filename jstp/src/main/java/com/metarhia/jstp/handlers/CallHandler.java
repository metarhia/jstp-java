package com.metarhia.jstp.handlers;

import com.metarhia.jstp.connection.JSCallback;
import com.metarhia.jstp.connection.JSTPConnection;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSTypes.JSArray;
import com.metarhia.jstp.core.JSTypes.JSNumber;
import com.metarhia.jstp.core.JSTypes.JSObject;
import com.metarhia.jstp.core.JSTypes.JSValue;

/**
 * Created by lundibundi on 3/12/17.
 */
public abstract class CallHandler implements ManualHandler {

  private long callbackNumber;

  @Override
  public void invoke(JSValue value) {
    final JSObject packet = (JSObject) value;
    JSArray callArray = (JSArray) packet.get(0);
    callbackNumber = (long) ((JSNumber) callArray.get(0)).getValue();
    handleCallback((JSArray) packet.get(1));
  }

  public abstract void handleCallback(JSArray data);

  protected void callback(JSTPConnection connection, JSCallback result, JSArray args) {
    connection.callback(result, args, callbackNumber);
  }
}
