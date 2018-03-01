package com.metarhia.jstp.messagehandling;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.exceptions.MessageHandlingException;

/**
 * Message handling interface that is used to parse messages received via
 * transport to be handled by the connection.
 */
public interface MessageHandler {

  /**
   * Adds message to the queue
   *
   * @param message message to be handled
   */
  void post(String message);

  /**
   * Clears current queue including currently running tasks
   */
  void clearQueue();

  /**
   * Sets message handling events listener
   *
   * @param listener event listener
   */
  void setListener(MessageHandlerListener listener);

  /**
   * Message handler event listener
   */
  interface MessageHandlerListener {

    /**
     * Called upon successful message processing
     * @param message parsed message
     */
    void onMessageParsed(JSObject message);

    /**
     * Called when error happened during message handling
     * @param e underlying error
     */
    void onHandlingError(MessageHandlingException e);
  }
}
