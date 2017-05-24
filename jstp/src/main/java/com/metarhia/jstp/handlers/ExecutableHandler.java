package com.metarhia.jstp.handlers;

import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import java.util.concurrent.Executor;

public abstract class ExecutableHandler implements ManualHandler, Runnable {

  private Executor executor;

  protected JSObject packet;

  public ExecutableHandler(Executor executor) {
    this.executor = executor;
  }

  @Override
  public void invoke(JSObject packet) {
    this.packet = packet;
    executor.execute(this);
  }
}
