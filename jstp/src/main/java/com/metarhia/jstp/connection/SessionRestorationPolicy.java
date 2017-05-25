package com.metarhia.jstp.connection;

import java.util.Queue;

/**
 * Created by lundibundi on 2/20/17.
 */
public class SessionRestorationPolicy implements RestorationPolicy {

  private boolean reconnectWhenTransportReady;

  public SessionRestorationPolicy() {
    this.reconnectWhenTransportReady = true;
  }

  @Override
  public boolean restore(Connection connection, Queue<Message> sendQueue) {
    for (Message message : sendQueue) {
      connection.send(message.getStringRepresentation(), true);
    }
    return true;
  }

  @Override
  public void onTransportAvailable(Connection connection, String appName, String sessionID) {
    if (!reconnectWhenTransportReady) {
      return;
    }
    if (sessionID != null) {
      connection.handshake(appName, sessionID, null);
    } else {
      connection.handshake(appName, null);
    }
  }

  public boolean isReconnectWhenTransportReady() {
    return reconnectWhenTransportReady;
  }

  public void setReconnectWhenTransportReady(boolean reconnectWhenTransportReady) {
    this.reconnectWhenTransportReady = reconnectWhenTransportReady;
  }
}
