package com.metarhia.jstp.handlers;

import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import java.util.List;

/**
 * Simple {@link ManualHandler} wrapper for easier event handling. Extracts event name
 * and arguments for users to avoid doing it by themselves every time.
 */
public abstract class EventHandler implements ManualHandler {

  @Override
  public void onMessage(JSObject message) {
    String eventName = message.getKey(1);
    handleEvent(eventName, (List<?>) message.get(eventName));
  }

  @Override
  public void onError(int errorCode) {
    // ignore by default
  }

  public abstract void handleEvent(String eventName, List<?> data);
}
