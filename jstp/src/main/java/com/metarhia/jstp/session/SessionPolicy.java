package com.metarhia.jstp.session;

import com.metarhia.jstp.connection.AppData;
import com.metarhia.jstp.connection.Connection;
import com.metarhia.jstp.connection.Message;
import com.metarhia.jstp.connection.MessageType;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.storage.StorageInterface;
import java.util.Map;

public interface SessionPolicy {

  /**
   * Called upon new connection to the remote site (not restore), hence Policy must take
   * appropriate actions to adapt, such as setting new app data,
   * resetting session data and counters.
   *
   * @param app         application that have been connected
   * @param sessionId   new session id
   * @param oldHandlers handlers of the previous session
   */
  void onNewConnection(AppData app, String sessionId, Map<Long, ManualHandler> oldHandlers);

  /**
   * Called after restoration handshake so that this client may resend buffered messages
   *
   * @param numServerReceivedMessages number off messages received on the server side
   */
  void restore(long numServerReceivedMessages);

  /**
   * Called when transport signalled that it has been connected
   * Should use one of Connection#handshake methods to connect to remote site
   */
  void onTransportAvailable();

  void onConnectionClosed();

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

  /**
   * Sets connection associated with this session
   */
  void setConnection(Connection connection);
}
