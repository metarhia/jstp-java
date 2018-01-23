package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSSerializer;
import com.metarhia.jstp.core.JSTypes.IndexedHashMap;
import com.metarhia.jstp.core.JSTypes.JSTypesUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Message implements Serializable {

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

  private MessageType type;

  /**
   * Implementation will by no means invalidate this value, it is the responsibility of the user to
   * keep this Optional field consistent
   */
  private String stringRepresentation;

  private Message() {
    this.message = new IndexedHashMap<>(2);
  }

  public Message(long messageNumber, MessageType type) {
    this();

    this.messageNumber = messageNumber;
    this.type = type;

    this.protocolArgs = new ArrayList<>(2);
    this.protocolArgs.add(this.messageNumber);

    message.put(type.getName(), this.protocolArgs);
  }

  public Message(JSObject<Object> message, MessageType type) {
    this();

    this.messageNumber = JSTypesUtil.<Double>getMixed(message, 0.0, 0).longValue();
    this.type = type;

    this.protocolArgs = (List<Object>) message.getByIndex(0);
    this.message = message;
  }

  public Message addProtocolArg(Object value) {
    this.protocolArgs.add(value);
    return this;
  }

  public <T> T getProtocolArg(int index) {
    return (T) protocolArgs.get(index);
  }

  public <T> T getArg(String key) {
    return (T) message.get(key);
  }

  public String getKey(int index) {
    return message.getKey(index);
  }

  public Message putArg(String key, Object value) {
    message.put(key, value);
    return this;
  }

  public Message putArgs(String key, Object... values) {
    message.put(key, Arrays.asList(values));
    return this;
  }

  public Message setMessageNumber(int messageNumber) {
    this.messageNumber = messageNumber;
    this.protocolArgs.set(0, this.messageNumber);
    return this;
  }

  public JSObject<Object> get() {
    return message;
  }

  public String stringify() {
    stringRepresentation = JSSerializer.stringify(message);
    return stringRepresentation;
  }

  public long getMessageNumber() {
    return messageNumber;
  }

  public String getStringRepresentation() {
    return stringRepresentation;
  }

  public MessageType getType() {
    return type;
  }

  public void setType(MessageType type) {
    this.type = type;
  }

  public void setStringRepresentation(String stringRepresentation) {
    this.stringRepresentation = stringRepresentation;
  }
}
