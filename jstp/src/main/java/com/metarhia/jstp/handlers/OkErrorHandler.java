package com.metarhia.jstp.handlers;

import com.metarhia.jstp.connection.JSCallback;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.exceptions.MessageHandlingException;
import java.util.Collections;
import java.util.List;

public abstract class OkErrorHandler implements ManualHandler {

  @Override
  public void onMessage(JSObject message) {
    if (message.isEmpty()) {
      handleOk(Collections.EMPTY_LIST);
      return;
    }
    String key = message.getKey(1);
    List<?> data = (List<?>) message.get(key);
    JSCallback callStatus = JSCallback.fromString(key);
    if (callStatus == JSCallback.OK) {
      handleOk(data);
    } else if (callStatus == JSCallback.ERROR) {
      Integer errorCode = null;
      if (!data.isEmpty()) {
        // first element must always be error code
        errorCode = (Integer) data.remove(0);
      }
      handleError(errorCode, data);
    } else {
      throw new MessageHandlingException("Invalid call status (not OK or ERROR)");
    }
  }

  @Override
  public void onError(int errorCode) {
    handleError(errorCode, Collections.EMPTY_LIST);
  }

  public abstract void handleOk(List<?> data);

  public abstract void handleError(Integer errorCode, List<?> data);
}
