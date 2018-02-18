package com.metarhia.jstp.core.Handlers;

import com.metarhia.jstp.core.JSInterfaces.JSObject;

/**
 * Created by lundibundi on 8/7/16.
 */
public interface ManualHandler {

  void onMessage(JSObject message);

  void onError(int errorCode);
}
