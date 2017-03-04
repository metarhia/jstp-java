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
  public boolean restore(JSTPConnection connection, Queue<JSTPMessage> sendQueue) {
    for (JSTPMessage message : sendQueue) {
      connection.send(message.getStringRepresentation());
    }
    return true;
  }

  @Override
  public void onTransportAvailable(JSTPConnection connection, String appName, String sessionID) {
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
