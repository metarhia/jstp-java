package com.metarhia.jstp.connection;

import java.util.Queue;

/**
 * Created by lundibundi on 2/20/17.
 */
public class DropRestorationPolicy implements RestorationPolicy {

  private boolean reconnectWhenTransportReady;

  public DropRestorationPolicy() {
    this.reconnectWhenTransportReady = true;
  }

  @Override
  public boolean restore(Connection connection, Queue<Message> sendQueue) {
    sendQueue.clear();
    return false;
  }

  @Override
  public void onTransportAvailable(Connection connection, String appName, String sessionID) {
    if (!reconnectWhenTransportReady) {
      return;
    }
    connection.handshake(appName, null);
  }

  public boolean isReconnectWhenTransportReady() {
    return reconnectWhenTransportReady;
  }

  public void setReconnectWhenTransportReady(boolean reconnectWhenTransportReady) {
    this.reconnectWhenTransportReady = reconnectWhenTransportReady;
  }
}
