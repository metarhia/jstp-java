package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.exceptions.ConnectionException;

/**
 * Created by lundibundi on 2/22/17.
 */
public class SimpleConnectionListener implements ConnectionListener {

  @Override
  public void onConnected(boolean restored) {
  }

  @Override
  public void onMessageRejected(JSObject message) {
  }

  @Override
  public void onConnectionError(ConnectionException error) {
  }

  @Override
  public void onConnectionClosed() {
  }
}
