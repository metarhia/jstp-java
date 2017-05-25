package com.metarhia.jstp.connection;

/**
 * Denotes possible {@link Connection} states
 */
public enum ConnectionState {
  STATE_AWAITING_HANDSHAKE,
  STATE_CONNECTED,
  STATE_AWAITING_RECONNECT,
  STATE_CLOSING,
  STATE_CLOSED
}
