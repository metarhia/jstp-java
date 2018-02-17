package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSInterfaces.JSObject;

public interface AbstractSocket {

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

    void onMessageReceived(JSObject message);

    void onSocketClosed();

    void onError(Exception e);
  }
}
