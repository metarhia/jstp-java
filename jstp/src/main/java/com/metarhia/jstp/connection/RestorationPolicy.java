package com.metarhia.jstp.connection;

import java.util.Queue;

/**
 * Created by lundibundi on 2/20/17.
 */
public interface RestorationPolicy {

  /**
   * Called after restoration handshake so that this client may resend packets
   *
   * @param connection connection to work with
   * @param sendQueue connection buffer of packets that was sent but was not received
   * @return true if session was restored, false otherwise
   */
  boolean restore(JSTPConnection connection, Queue<JSTPMessage> sendQueue);

  /**
   * Called when transport signalled that it has been connected
   *
   * @param connection connection to work with
   * @param appName current name of the application that this connection is associated with
   * @param sessionID current session or null if there was no session
   */
  void onTransportAvailable(JSTPConnection connection, String appName, String sessionID);
}
