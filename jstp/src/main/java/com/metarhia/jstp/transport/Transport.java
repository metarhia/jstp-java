package com.metarhia.jstp.transport;

/**
 * Interface for JSTP transport
 */
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

  /**
   * Closes the connection
   *
   * @param forced if true the connection should be closed immediately, otherwise it may perform
   *               additional operations, such as writing messages remaining in the queue or
   *               handling remaining incoming data
   */
  void close(boolean forced);

  /**
   * Clears the message queue
   */
  void clearQueue();

  /**
   * Sets transport event listener
   *
   * @param listener event listener
   */
  void setListener(TransportListener listener);

  /**
   * Checks if transport is connected
   *
   * @return true if transport is connected and false otherwise
   */
  boolean isConnected();

  /**
   * Checks if transport is closed
   *
   * @return true if transport is closed and false otherwise
   */
  boolean isClosed();

  /**
   * Transport event listener interface
   */
  interface TransportListener {

    /**
     * Called when connection gets established
     */
    void onTransportConnected();

    /**
     * Called when message is received
     *
     * @param message message received over the connection
     */
    void onMessageReceived(String message);

    /**
     * Called when connection gets closed
     */
    void onTransportClosed();

    /**
     * Called when error happened in the transport with possibly not null exception parameter
     *
     * @param e exception accompanying the error
     */
    void onTransportError(Exception e);
  }
}
