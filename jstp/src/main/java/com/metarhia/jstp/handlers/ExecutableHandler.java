package com.metarhia.jstp.handlers;

import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import java.util.concurrent.Executor;

/**
 * Message handler wrapper that allows to handle messages using specified executor
 */
public class ExecutableHandler implements ManualHandler, Runnable {

  private Executor executor;

  private final ManualHandler handler;

  private JSObject message;

  private Integer errorCode;

  /**
   * Creates new executable handler instance
   *
   * @param executor executor to handle messages
   * @param handler  handler to be wrapped
   */
  public ExecutableHandler(Executor executor, ManualHandler handler) {
    this.executor = executor;
    this.handler = handler;
  }

  @Override
  public void run() {
    if (message != null) {
      handler.onMessage(message);
    } else {
      handler.onError(errorCode);
    }
  }

  @Override
  public void onMessage(JSObject message) {
    this.message = message;
    executor.execute(this);
  }

  @Override
  public void onError(int errorCode) {
    this.errorCode = errorCode;
    executor.execute(this);
  }
}
