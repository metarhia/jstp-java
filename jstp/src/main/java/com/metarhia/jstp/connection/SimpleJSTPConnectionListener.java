package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSTypes.JSObject;

/**
 * Created by lundibundi on 2/22/17.
 */
public class SimpleJSTPConnectionListener implements JSTPConnectionListener {

  @Override
  public void onConnected(boolean restored) {
  }

  @Override
  public void onPacketRejected(JSObject packet) {
  }

  @Override
  public void onConnectionError(int errorCode) {
  }

  @Override
  public void onConnectionClosed() {
  }
}
