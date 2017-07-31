package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSSerializer;
import com.metarhia.jstp.core.JSTypes.IndexedHashMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Helper class to construct message to be sent via {@link Connection}
 */
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

  /*
   * Type of the message
   */
  private MessageType type;

  /**
   * Implementation will by no means invalidate this value, it is the responsibility of the user to
   * keep this Optional field consistent
   */
  private String stringRepresentation;

  private Message() {
    this.message = new IndexedHashMap<>(2);
  }

  /**
   * Creates new message with specified message number {@param messageNumber} of type
   * {@param type}
   *
   * @param messageNumber number of the message
   * @param type          type of the message
   */
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

    this.messageNumber = Connection.getMessageNumber(message);
    this.type = type;

    this.protocolArgs = (List<Object>) message.getByIndex(0);
    this.message = message;
  }

  /**
   * Adds protocol argument
   *
   * @param value protocol argument
   *
   * @return current message instance
   */
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

  /**
   * Adds argument
   *
   * @param key   argument key
   * @param value argument value
   *
   * @return current message instance
   */
  public Message putArg(String key, Object value) {
    message.put(key, value);
    return this;
  }

  public Message putArgs(String key, Object... values) {
    message.put(key, Arrays.asList(values));
    return this;
  }

  /**
   * Changes message number to {@param messageNumber}
   *
   * @param messageNumber new number of the message
   *
   * @return current message instance
   */
  public Message setMessageNumber(long messageNumber) {
    this.messageNumber = messageNumber;
    this.protocolArgs.set(0, this.messageNumber);
    return this;
  }

  /**
   * Gets message as a {@link JSObject}
   *
   * @return message
   */
  public JSObject<Object> get() {
    return message;
  }

  public String stringify() {
    stringRepresentation = JSSerializer.stringify(message);
    return stringRepresentation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Message message1 = (Message) o;
    return Objects.equals(message, message1.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message);
  }

  /**
   * Gets message number
   *
   * @return message number
   */
  public long getMessageNumber() {
    return messageNumber;
  }

  /**
   * Gets string representation of message
   *
   * @return string representation of message
   */
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
