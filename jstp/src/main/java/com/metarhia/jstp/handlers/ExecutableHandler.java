package com.metarhia.jstp.handlers;

import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import java.util.concurrent.Executor;

public abstract class ExecutableHandler implements ManualHandler, Runnable {

  private Executor executor;

  protected JSObject message;

  public ExecutableHandler(Executor executor) {
    this.executor = executor;
  }

  @Override
  public void handle(JSObject message) {
    this.message = message;
    executor.execute(this);
  }
}
