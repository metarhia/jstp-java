package com.metarhia.jstp.handlers;

import com.metarhia.jstp.connection.JSCallback;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.exceptions.MessageHandlingException;
import java.util.List;

public abstract class OkErrorHandler implements ManualHandler {

  @Override
  public void handle(JSObject message) {
    String key = message.getKey(1);
    List<?> data = (List<?>) message.get(key);
    JSCallback callStatus = JSCallback.fromString(key);
    if (callStatus == JSCallback.OK) {
      handleOk(data);
    } else if (callStatus == JSCallback.ERROR) {
      handleError(data);
    } else {
      throw new MessageHandlingException("Invalid call status (not OK or ERROR)");
    }
  }

  public abstract void handleOk(List<?> data);

  public abstract void handleError(List<?> data);
}
