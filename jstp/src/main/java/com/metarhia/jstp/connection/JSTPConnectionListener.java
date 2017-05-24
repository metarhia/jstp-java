package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSInterfaces.JSObject;

/**
 * Created by lundibundi on 2/22/17.
 */
public interface JSTPConnectionListener {

  void onConnected(boolean restored);

  void onMessageRejected(JSObject message);

  void onConnectionError(int errorCode);

  void onConnectionClosed();
}
