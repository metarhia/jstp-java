package com.metarhia.jstp.core.Handlers;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by lundibundi on 8/8/16.
 */
public abstract class Handler<T> implements ManualHandler {

  protected List<T> handlers;

  public Handler() {
    this.handlers = new LinkedList<>();
  }

  public Handler(T handler) {
    this();
    this.handlers.add(handler);
  }

  public Handler(Collection<T> handlers) {
    this.handlers = new LinkedList<>(handlers);
  }

  public void addHandler(T handler) {
    this.handlers.add(handler);
  }

  public void removeHandler(T handler) {
    this.handlers.remove(handler);
  }
}
