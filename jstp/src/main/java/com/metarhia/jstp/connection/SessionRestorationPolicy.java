package com.metarhia.jstp.connection;

import java.util.Queue;

/**
 * Created by lundibundi on 2/20/17.
 */
public class SessionRestorationPolicy implements RestorationPolicy {

  private JSTPConnection connection;

  public SessionRestorationPolicy(JSTPConnection connection) {
    this.connection = connection;
  }

  @Override
  public boolean restore(Queue<JSTPMessage> sendQueue) {
    for (JSTPMessage message : sendQueue) {
      connection.send(message.getStringRepresantation());
    }
    return true;
  }

  @Override
  public void onTransportAvailable(String appName, String sessionID) {
    if (sessionID != null) {
      connection.handshake(appName, sessionID, null);
    } else {
      connection.handshake(appName, null);
    }
  }
}
