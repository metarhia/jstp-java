package com.metarhia.jstp.handlers;

import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import java.util.concurrent.Executor;

public abstract class ExecutableHandler implements ManualHandler, Runnable {

  private Executor executor;

  protected JSObject message;

  protected Integer errorCode;

  public ExecutableHandler(Executor executor) {
    this.executor = executor;
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
