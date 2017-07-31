package com.metarhia.jstp.handlers;

import com.metarhia.jstp.connection.Connection;
import com.metarhia.jstp.connection.JSCallback;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import java.util.List;

/**
 * Handler for incoming call messages
 */
public abstract class CallHandler implements ManualHandler {

  private long callbackNumber;

  @Override
  public void onMessage(JSObject message) {
    callbackNumber = Connection.getMessageNumber(message);
    String methodName = message.getKey(1);
    handleCall(methodName, (List<?>) message.get(methodName));
  }

  @Override
  public void onError(int errorCode) {
    // ignore by default
  }

  /**
   * Handles incoming call message
   *
   * @param methodName name of the called method
   * @param data       call data
   */
  public abstract void handleCall(String methodName, List<?> data);

  /**
   * Sends callback message using connection {@param connection}
   *
   * @param connection connection to send callback on
   * @param result     callback result (see {@link JSCallback})
   * @param args       callback arguments
   */
  public void callback(Connection connection, JSCallback result, List<?> args) {
    connection.callback(result, args, callbackNumber);
  }
}
