package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSInterfaces.JSObject;

/**
 * Created by lundibundi on 2/22/17.
 */
public class SimpleJSTPConnectionListener implements JSTPConnectionListener {

  @Override
  public void onConnected(boolean restored) {
  }

  @Override
  public void onMessageRejected(JSObject message) {
  }

  @Override
  public void onConnectionError(int errorCode) {
  }

  @Override
  public void onConnectionClosed() {
  }
}
