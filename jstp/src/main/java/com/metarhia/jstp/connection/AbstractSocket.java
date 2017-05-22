package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSInterfaces.JSObject;

public interface AbstractSocket {

  /**
   * @return true if connect task was committed (it doesn't mean that connection was established)
   * else returns false if error occurred
   */
  boolean connect();

  void send(String message);

  void pause();

  void resume();

  void close(boolean forced);

  void clearQueue();

  int getQueueSize();

  void setSocketListener(AbstractSocketListener listener);

  boolean isConnected();

  boolean isClosed();

  boolean isRunning();

  interface AbstractSocketListener {

    void onConnected();

    void onPacketReceived(JSObject packet);

    void onConnectionClosed();

    void onError(Exception e);
  }
}
