package com.metarhia.jstp.connection;

import java.util.Queue;

/**
 * Created by lundibundi on 2/20/17.
 */
public interface RestorationPolicy {

  /**
   * Called after restoration handshake so that this client may resend packets
   *
   * @param sendQueue connection buffer of packets that was sent but was not received
   * @return true if session was restored, false otherwise
   */
  boolean restore(Queue<JSTPMessage> sendQueue);

  /**
   * Called when transport signalled that it has been connected
   *
   * @param appName current name of the application that this connection is associated with
   * @param sessionID current session or null if there was no session
   */
  void onTransportAvailable(String appName, String sessionID);
}
