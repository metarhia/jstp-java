package com.metarhia.jstp.connection;

public enum MessageType {
  HANDSHAKE("handshake"),
  CALL("call"),
  CALLBACK("callback"),
  EVENT("event"),
  STREAM("stream"),
  INSPECT("inspect"),
  PING("ping"),
  PONG("pong");

  private final String name;

  MessageType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static MessageType fromString(String name) {
    for (MessageType t : MessageType.values()) {
      if (t.name.equals(name)) {
        return t;
      }
    }
    return null;
  }
}
