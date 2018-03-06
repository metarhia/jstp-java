package com.metarhia.jstp.session;

import com.metarhia.jstp.Constants;
import com.metarhia.jstp.connection.AppData;
import com.metarhia.jstp.connection.Connection;
import com.metarhia.jstp.connection.Message;
import com.metarhia.jstp.connection.MessageType;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.storage.StorageInterface;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class DropSessionPolicy implements SessionPolicy, Serializable {

  private static final long serialVersionUID = -346678830813726170L;

  private SessionData sessionData;

  private boolean reconnectWhenTransportReady;

  transient private Connection connection;

  public DropSessionPolicy() {
    this.reconnectWhenTransportReady = true;
    this.sessionData = new SessionData();
  }

  public static DropSessionPolicy restoreFrom(StorageInterface storageInterface) {
    DropSessionPolicy sessionPolicy = new DropSessionPolicy();
    sessionPolicy.restoreSession(storageInterface);
    return sessionPolicy;
  }

  @Override
  public void restore(long numServerReceivedMessages) {
    // ignore
  }

  @Override
  public void onNewConnection(AppData app, String sessionId, Map<Long, ManualHandler> oldHandlers) {
    sessionData.resetCounters();
    sessionData.setParameters(app, sessionId);
  }

  @Override
  public void onTransportAvailable() {
    if (!reconnectWhenTransportReady) {
      return;
    }
    connection.handshake(sessionData.getAppData(), null);
  }

  @Override
  public void onConnectionClosed() {
    // ignore
  }

  @Override
  public void onMessageSent(Message message) {
    // ignore
  }

  @Override
  public void onMessageReceived(JSObject message, MessageType type) {
    // ignore
  }

  @Override
  public SessionData getSessionData() {
    return sessionData;
  }

  public void setSessionData(SessionData sessionData) {
    this.sessionData = sessionData;
  }

  @Override
  public void saveSession(StorageInterface storageInterface) {
    storageInterface.putSerializable(
        Constants.KEY_SESSION + DropSessionPolicy.class.getCanonicalName(), this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DropSessionPolicy that = (DropSessionPolicy) o;
    return reconnectWhenTransportReady == that.reconnectWhenTransportReady &&
        Objects.equals(sessionData, that.sessionData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sessionData, reconnectWhenTransportReady);
  }

  @Override
  public void restoreSession(StorageInterface storageInterface) {
    DropSessionPolicy sessionPolicy = (DropSessionPolicy) storageInterface.getSerializable(
        Constants.KEY_SESSION + DropSessionPolicy.class.getCanonicalName(), this);
    reconnectWhenTransportReady = sessionPolicy.reconnectWhenTransportReady;
    sessionData = sessionPolicy.sessionData;
  }

  @Override
  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  public boolean isReconnectWhenTransportReady() {
    return reconnectWhenTransportReady;
  }

  public void setReconnectWhenTransportReady(boolean reconnectWhenTransportReady) {
    this.reconnectWhenTransportReady = reconnectWhenTransportReady;
  }
}
