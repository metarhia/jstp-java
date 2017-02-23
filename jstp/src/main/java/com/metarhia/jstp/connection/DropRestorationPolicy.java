package com.metarhia.jstp.connection;

import java.util.Queue;

/**
 * Created by lundibundi on 2/20/17.
 */
public class DropRestorationPolicy implements RestorationPolicy {

  private JSTPConnection connection;

  public DropRestorationPolicy(JSTPConnection connection) {
    this.connection = connection;
  }

  @Override
  public boolean restore(Queue<JSTPMessage> sendQueue) {
    sendQueue.clear();
    return false;
  }

  @Override
  public void onTransportAvailable(String appName, String sessionID) {
    connection.handshake(appName, null);
  }
}
