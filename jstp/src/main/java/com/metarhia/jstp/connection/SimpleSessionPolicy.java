package com.metarhia.jstp.connection;

import com.metarhia.jstp.Constants;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.storage.StorageInterface;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SimpleSessionPolicy implements SessionPolicy, Serializable {

  private long sendBufferCapacity;

  private boolean reconnectWhenTransportReady;

  private ConcurrentLinkedQueue<String> sentMessages;

  private HashMap<String, Serializable> data;

  private SessionData sessionData;

  public SimpleSessionPolicy() {
    this.reconnectWhenTransportReady = true;
    this.sendBufferCapacity = 100;
    this.sessionData = new SessionData();
    this.data = new HashMap<>();
    this.sentMessages = new ConcurrentLinkedQueue<>();
  }

  public static SimpleSessionPolicy restoreFrom(StorageInterface storageInterface) {
    SimpleSessionPolicy sessionPolicy = new SimpleSessionPolicy();
    sessionPolicy.restoreSession(storageInterface);
    return sessionPolicy;
  }

  @Override
  public boolean restore(Connection connection, long numServerReceivedMessages) {
    long redundantMessages =
        sentMessages.size() - (sessionData.getNumSentMessages() - numServerReceivedMessages);
    while (redundantMessages-- > 0) {
      sentMessages.poll();
    }
    for (String message : sentMessages) {
      connection.send(message);
    }
    sessionData.setNumReceivedMessages(numServerReceivedMessages);
    return true;
  }

  @Override
  public void reset(String appName) {
    if (appName != null) {
      sessionData.setAppName(appName);
    }
    sessionData.resetCounters();
    data.clear();
    sentMessages.clear();
  }

  @Override
  public void onMessageReceived(JSObject message, MessageType type) {
    if (type == MessageType.CALLBACK || type == MessageType.PONG) {
      long messageNumber = Connection.getMessageNumber(message);
      sessionData.setNumReceivedMessages(messageNumber);
    }
  }

  @Override
  public void onTransportAvailable(Connection connection) {
    if (!reconnectWhenTransportReady) {
      return;
    }
    if (sessionData.getSessionId() != null) {
      connection.handshake(sessionData.getAppName(), sessionData.getSessionId(), null);
    } else {
      connection.handshake(sessionData.getAppName(), null);
    }
  }

  @Override
  public void onMessageSent(Message message) {
    String msg = message.getStringRepresentation();
    if (msg == null) {
      msg = message.stringify();
    }
    sessionData.setNumSentMessages(message.getMessageNumber());
    sentMessages.add(msg);
    if (sentMessages.size() >= sendBufferCapacity) {
      sentMessages.poll();
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
        sentMessages.containsAll(Arrays.asList(that.sentMessages.toArray())) &&
        Objects.equals(data, that.data) &&
        Objects.equals(sessionData, that.sessionData);
  }

  @Override
  public int hashCode() {

    return Objects
        .hash(sendBufferCapacity, reconnectWhenTransportReady, sentMessages, data, sessionData);
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

  public ConcurrentLinkedQueue<String> getSentMessages() {
    return sentMessages;
  }

  @Override
  public SessionData getSessionData() {
    return sessionData;
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
