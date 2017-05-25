package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSTypes.IndexedHashMap;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import java.util.ArrayList;
import java.util.List;

public class Message {

  /**
   * Generated message
   */
  private JSObject<Object> message;

  /**
   * Arguments specific for protocol (always contains messageNumber)
   */
  private List<Object> protocolArgs;

  /**
   * Number of package corresponding to this message
   */
  private long messageNumber;

  /**
   * Implementation will by no means invalidate this value, it is the responsibility of the user to
   * keep this Optional field consistent
   */
  private String stringRepresentation;

  private Message() {
    this.message = new IndexedHashMap<>(2);
  }

  public Message(long messageNumber, String type) {
    this();

    this.messageNumber = messageNumber;

    this.protocolArgs = new ArrayList<>(2);
    this.protocolArgs.add(this.messageNumber);

    message.put(type, this.protocolArgs);
  }

  public Message addProtocolArg(Object value) {
    this.protocolArgs.add(value);
    return this;
  }

  public Message putArg(String key, Object value) {
    message.put(key, value);
    return this;
  }

  public Message setMessageNumber(int messageNumber) {
    this.messageNumber = messageNumber;
    this.protocolArgs.set(0, this.messageNumber);
    return this;
  }

  public JSObject<Object> getMessage() {
    return message;
  }

  public long getMessageNumber() {
    return messageNumber;
  }

  public String getStringRepresentation() {
    return stringRepresentation;
  }

  public void setStringRepresentation(String stringRepresentation) {
    this.stringRepresentation = stringRepresentation;
  }
}
