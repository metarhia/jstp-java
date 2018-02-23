package com.metarhia.jstp.transport;

import com.metarhia.jstp.core.JSInterfaces.JSObject;

public interface Transport {

  /**
   * @return true if connect task was committed (it doesn't mean that connection was established)
   * else returns false if error occurred
   */
  boolean connect();

  /**
   * Sends message and adds message separator after it.
   *
   * @param message string to be sent
   */
  void send(String message);

  void close(boolean forced);

  void clearQueue();

  void setListener(TransportListener listener);

  boolean isConnected();

  boolean isClosed();

  interface TransportListener {

    void onTransportConnected();

    void onMessageReceived(JSObject message);

    void onTransportClosed();

    void onTransportError(Exception e);
  }
}
