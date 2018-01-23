package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.storage.StorageInterface;

public class DropSessionPolicy implements SessionPolicy {

  private SessionData sessionData;

  private boolean reconnectWhenTransportReady;

  public DropSessionPolicy() {
    this.reconnectWhenTransportReady = true;
    this.sessionData = new SessionData();
  }

  @Override
  public boolean restore(Connection connection, long numServerReceivedMessages) {
    return false;
  }

  @Override
  public void reset(String appName) {
    if (appName != null) {
      sessionData.setAppName(appName);
    }
    sessionData.resetCounters();
  }

  @Override
  public void onTransportAvailable(Connection connection) {
    if (!reconnectWhenTransportReady) {
      return;
    }
    connection.handshake(sessionData.getAppName(), null);
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

  @Override
  public void saveSession(StorageInterface storageInterface) {
    // ignore
  }

  @Override
  public void restoreSession(StorageInterface storageInterface) {
    // ignore
  }

  public boolean isReconnectWhenTransportReady() {
    return reconnectWhenTransportReady;
  }

  public void setReconnectWhenTransportReady(boolean reconnectWhenTransportReady) {
    this.reconnectWhenTransportReady = reconnectWhenTransportReady;
  }
}
