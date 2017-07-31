package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSInterfaces.JSObject;

/**
 * Simple connection listener that overwrites all {@link ConnectionListener} methods to allow a
 * user to only overwrite methods needed and avoid writing stubs for the other methods
 */
public class SimpleConnectionListener implements ConnectionListener {

  @Override
  public void onConnected(boolean restored) {
  }

  @Override
  public void onMessageRejected(JSObject message) {
  }

  @Override
  public void onConnectionClosed() {
  }
}
