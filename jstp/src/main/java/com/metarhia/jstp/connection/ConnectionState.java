package com.metarhia.jstp.connection;

/**
 * Denotes possible {@link Connection} states
 */
public enum ConnectionState {
  AWAITING_HANDSHAKE,
  AWAITING_HANDSHAKE_RESPONSE,
  CONNECTED,
  AWAITING_RECONNECT,
  CLOSING,
  CLOSED
}
