package com.metarhia.jstp.session;

import com.metarhia.jstp.Constants;
import com.metarhia.jstp.connection.AppData;
import com.metarhia.jstp.connection.Connection;
import com.metarhia.jstp.connection.ConnectionError;
import com.metarhia.jstp.connection.Message;
import com.metarhia.jstp.connection.MessageType;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.storage.StorageInterface;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;

public class SimpleSessionPolicy implements SessionPolicy, Serializable {

  private static final long serialVersionUID = -3453132564056499599L;

  private boolean reconnectWhenTransportReady;

  private Map<Long, Message> sentMessages;

  private long lastDeliveredNumber;

  private HashMap<String, Serializable> data;

  private SessionData sessionData;

  transient private Connection connection;

  public SimpleSessionPolicy() {
    this.reconnectWhenTransportReady = true;
    this.sessionData = new SessionData();
    this.data = new HashMap<>();
    this.sentMessages = new ConcurrentSkipListMap<>();
  }

  public static SimpleSessionPolicy restoreFrom(StorageInterface storageInterface) {
    SimpleSessionPolicy sessionPolicy = new SimpleSessionPolicy();
    sessionPolicy.restoreSession(storageInterface);
    return sessionPolicy;
  }

  @Override
  public void restore(long numServerReceivedMessages) {
    // remove and report handlers that were left out before connection broke
    Iterator<Entry<Long, ManualHandler>> it = connection.getHandlers().entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Long, ManualHandler> next = it.next();
      if (next.getKey() <= lastDeliveredNumber) {
        next.getValue().onError(ConnectionError.CALLBACK_LOST.getErrorCode());
        it.remove();
      }
    }

    removeBufferedMessages(numServerReceivedMessages);

    for (Message message : sentMessages.values()) {
      connection.send(message.getStringRepresentation());
    }
  }

  @Override
  public void onNewConnection(AppData app, String sessionId, Map<Long, ManualHandler> oldHandlers) {
    sessionData.resetCounters();
    sessionData.setParameters(app, sessionId);
    lastDeliveredNumber = 0;
    data.clear();
    sentMessages.clear();
  }

  @Override
  public void onMessageReceived(JSObject message, MessageType type) {
    long messageNumber = Connection.getMessageNumber(message);
    if (type == MessageType.CALLBACK || type == MessageType.PONG) {
      // we don't want to report error because we might receive a callback later
      removeBufferedMessages(messageNumber, null);
    }
    if (messageNumber > sessionData.getNumReceivedMessages()) {
      sessionData.setNumReceivedMessages(messageNumber);
    }
  }

  private void removeBufferedMessages(long lastDeliveredNumber) {
    removeBufferedMessages(lastDeliveredNumber,
        ConnectionError.CALLBACK_LOST.getErrorCode());
  }

  private void removeBufferedMessages(long lastDeliveredNumber, Integer errorCode) {
    for (long i = this.lastDeliveredNumber + 1; i <= lastDeliveredNumber; ++i) {
      sentMessages.remove(i);
      if (errorCode != null) {
        ManualHandler handler = connection.removeHandler(i);
        if (handler != null) {
          handler.onError(errorCode);
        }
      }
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
  public void onConnectionClosed() {
    // ignore for now
  }

  @Override
  public void onMessageSent(Message message) {
    if (message.getStringRepresentation() == null) {
      message.stringify();
    }
    long messageNumber = message.getMessageNumber();
    sessionData.setNumSentMessages(messageNumber);
    sentMessages.put(messageNumber, message);
  }

  @Override
  public void saveSession(StorageInterface storageInterface) {
    storageInterface.putSerializable(Constants.KEY_SESSION, this);
  }

  @Override
  public void restoreSession(StorageInterface storageInterface) {
    SimpleSessionPolicy sessionPolicy = (SimpleSessionPolicy)
        storageInterface.getSerializable(Constants.KEY_SESSION, this);
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
    return reconnectWhenTransportReady == that.reconnectWhenTransportReady &&
        lastDeliveredNumber == that.lastDeliveredNumber &&
        Objects.equals(sentMessages, that.sentMessages) &&
        Objects.equals(data, that.data) &&
        Objects.equals(sessionData, that.sessionData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reconnectWhenTransportReady, sentMessages, lastDeliveredNumber,
        data, sessionData);
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

  public Map<Long, Message> getSentMessages() {
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

  public boolean isReconnectWhenTransportReady() {
    return reconnectWhenTransportReady;
  }

  public void setReconnectWhenTransportReady(boolean reconnectWhenTransportReady) {
    this.reconnectWhenTransportReady = reconnectWhenTransportReady;
  }
}
