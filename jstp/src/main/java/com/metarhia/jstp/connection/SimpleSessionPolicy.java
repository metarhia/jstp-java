package com.metarhia.jstp.connection;

import com.metarhia.jstp.Constants;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.storage.StorageInterface;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SimpleSessionPolicy implements SessionPolicy, Serializable {

  private long sendBufferCapacity;

  private boolean reconnectWhenTransportReady;

  private HashMap<Long, String> sentMessages;

  private long lastDeliveredNumber;

  private HashMap<String, Serializable> data;

  private SessionData sessionData;

  transient private Connection connection;

  public SimpleSessionPolicy() {
    this.reconnectWhenTransportReady = true;
    this.sendBufferCapacity = 0;
    this.sessionData = new SessionData();
    this.data = new HashMap<>();
    this.sentMessages = new HashMap<>();
  }

  public static SimpleSessionPolicy restoreFrom(StorageInterface storageInterface) {
    SimpleSessionPolicy sessionPolicy = new SimpleSessionPolicy();
    sessionPolicy.restoreSession(storageInterface);
    return sessionPolicy;
  }

  @Override
  public boolean restore(long numServerReceivedMessages) {
    removeBufferedMessages(numServerReceivedMessages);
    for (String message : sentMessages.values()) {
      connection.send(message);
    }
    return true;
  }

  @Override
  public void reset(String app) {
    if (app != null) {
      sessionData.setParameters(AppData.valueOf(app), null);
    }
    lastDeliveredNumber = 0;
    sessionData.resetCounters();
    data.clear();
    sentMessages.clear();
  }

  @Override
  public void onMessageReceived(JSObject message, MessageType type) {
    long messageNumber = Connection.getMessageNumber(message);
    if (type == MessageType.CALLBACK || type == MessageType.PONG) {
      removeBufferedMessages(messageNumber);
    }
    if (messageNumber > sessionData.getNumReceivedMessages()) {
      sessionData.setNumReceivedMessages(messageNumber);
    }
  }

  private void removeBufferedMessages(long lastDeliveredNumber) {
    for (long i = this.lastDeliveredNumber + 1; i <= lastDeliveredNumber; ++i) {
      sentMessages.remove(i);
      connection.removeHandler(i);
    }
    this.lastDeliveredNumber = lastDeliveredNumber;
  }

  @Override
  public void onTransportAvailable() {
    if (!reconnectWhenTransportReady) {
      return;
    }
    if (sessionData.getSessionId() != null) {
      connection.handshake(sessionData.getAppData(), sessionData.getSessionId(), null);
    } else {
      connection.handshake(sessionData.getAppData(), null);
    }
  }

  @Override
  public void onMessageSent(Message message) {
    String msg = message.getStringRepresentation();
    if (msg == null) {
      msg = message.stringify();
    }
    sessionData.setNumSentMessages(message.getMessageNumber());
    sentMessages.put(message.getMessageNumber(), msg);
    if (sendBufferCapacity > 0 && sentMessages.size() >= sendBufferCapacity) {
      removeBufferedMessages(lastDeliveredNumber + 1);
    }
  }

  @Override
  public void saveSession(StorageInterface storageInterface) {
    storageInterface.putSerializable(Constants.KEY_SESSION, this);
  }

  @Override
  public void restoreSession(StorageInterface storageInterface) {
    SimpleSessionPolicy sessionPolicy = (SimpleSessionPolicy)
        storageInterface.getSerializable(Constants.KEY_SESSION, this);
    sendBufferCapacity = sessionPolicy.sendBufferCapacity;
    reconnectWhenTransportReady = sessionPolicy.reconnectWhenTransportReady;
    lastDeliveredNumber = sessionPolicy.lastDeliveredNumber;
    sentMessages = sessionPolicy.sentMessages;
    data = sessionPolicy.data;
    sessionData = sessionPolicy.sessionData;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SimpleSessionPolicy that = (SimpleSessionPolicy) o;
    return sendBufferCapacity == that.sendBufferCapacity &&
        reconnectWhenTransportReady == that.reconnectWhenTransportReady &&
        lastDeliveredNumber == that.lastDeliveredNumber &&
        Objects.equals(sentMessages, that.sentMessages) &&
        Objects.equals(data, that.data) &&
        Objects.equals(sessionData, that.sessionData);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(sendBufferCapacity, reconnectWhenTransportReady, sentMessages,
            sentMessages, data, sessionData);
  }

  public Serializable get(String o) {
    return data.get(o);
  }

  public Serializable put(String s, Serializable serializable) {
    return data.put(s, serializable);
  }

  public Map<String, Serializable> getData() {
    return data;
  }

  public HashMap<Long, String> getSentMessages() {
    return sentMessages;
  }

  @Override
  public SessionData getSessionData() {
    return sessionData;
  }

  @Override
  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  public void setSessionData(SessionData sessionData) {
    this.sessionData = sessionData;
  }

  public long getSendBufferCapacity() {
    return sendBufferCapacity;
  }

  public void setSendBufferCapacity(long sendBufferCapacity) {
    this.sendBufferCapacity = sendBufferCapacity;
  }

  public boolean isReconnectWhenTransportReady() {
    return reconnectWhenTransportReady;
  }

  public void setReconnectWhenTransportReady(boolean reconnectWhenTransportReady) {
    this.reconnectWhenTransportReady = reconnectWhenTransportReady;
  }
}
