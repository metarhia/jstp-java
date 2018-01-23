package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.storage.StorageInterface;

public interface SessionPolicy {

  /**
   * Called after restoration handshake so that this client may resend buffered messages
   *
   * @param connection                connection to work with
   * @param numServerReceivedMessages number off messages received on the server side
   *
   * @return true if session was restored, false otherwise
   */
  boolean restore(Connection connection, long numServerReceivedMessages);

  /**
   * Reset current session counters and buffers
   *
   * @param appName new application name if needed (if null it's ignored)
   */
  void reset(String appName);

  /**
   * Called when transport signalled that it has been connected
   * Should use one of Connection#handshake methods to connect to remote site
   *
   * @param connection connection to work with
   */
  void onTransportAvailable(Connection connection);

  /**
   * Called by {@link Connection} upon each message available for buffering being send
   *
   * @param message sent message
   */
  void onMessageSent(Message message);

  /**
   * Called by {@link Connection} upon each received message
   *
   * @param message received message
   * @param type    message type
   */
  void onMessageReceived(JSObject message, MessageType type);

  /**
   * Saves session to {@param storageInterface}
   *
   * @param storageInterface storage to save session to
   */
  void saveSession(StorageInterface storageInterface);

  /**
   * Restores session from {@param storageInterface}
   *
   * @param storageInterface storage to restore session from
   */
  void restoreSession(StorageInterface storageInterface);

  /**
   * @return session data
   */
  SessionData getSessionData();
}
