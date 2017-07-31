package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSInterfaces.JSObject;

/**
 * Listener for connection events
 */
public interface ConnectionListener {

  /**
   * Called when connection gets established
   *
   * @param restored true if connection was restored and false otherwise
   */
  void onConnected(boolean restored);

  /**
   * Called when message is rejected
   *
   * @param message rejected message
   */
  void onMessageRejected(JSObject message);

  /**
   * Called when connection gets closed
   */
  void onConnectionClosed();
}
