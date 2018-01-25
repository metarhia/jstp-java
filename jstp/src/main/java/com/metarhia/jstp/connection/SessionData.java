package com.metarhia.jstp.connection;

import java.io.Serializable;
import java.util.Objects;

/**
 * Contains information about session
 */
public class SessionData implements Serializable {

  /**
   * Application data of a current session
   */
  private AppData appData;

  /**
   * Session id as uuid4
   */
  private String sessionId;

  /**
   * Number of messages sent by this side of a connection
   */
  private long numSentMessages;

  /**
   * Number of messages confirmed to be received on the other side
   */
  private long numReceivedMessages;

  public SessionData() {
  }

  public SessionData(String app) {
    this(app, null);
  }

  public SessionData(AppData appData) {
    this(appData, null);
  }

  public SessionData(AppData appData, String sessionId) {
    this(appData, sessionId, 0, 0);
  }

  public SessionData(String app, String sessionId) {
    this(app, sessionId, 0, 0);
  }

  public SessionData(String app, String sessionId,
                     long numSentMessages, long numReceivedMessages) {
    this(AppData.valueOf(app), sessionId, numSentMessages, numReceivedMessages);
  }

  public SessionData(AppData appData, String sessionId,
                     long numSentMessages, long numReceivedMessages) {
    this.appData = appData != null ? appData : new AppData();
    this.sessionId = sessionId;
    this.numSentMessages = numSentMessages;
    this.numReceivedMessages = numReceivedMessages;
  }

  public void incrementNumSentMessages() {
    this.numSentMessages++;
  }

  public void incrementNumReceivedMessages() {
    this.numReceivedMessages++;
  }

  public void resetCounters() {
    this.numReceivedMessages = 0;
    this.numSentMessages = 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SessionData that = (SessionData) o;
    return numSentMessages == that.numSentMessages &&
        numReceivedMessages == that.numReceivedMessages &&
        Objects.equals(appData, that.appData) &&
        Objects.equals(sessionId, that.sessionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(appData, sessionId, numSentMessages, numReceivedMessages);
  }

  public AppData getAppData() {
    return appData;
  }

  public void setAppData(AppData appData) {
    this.appData = appData;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public long getNumSentMessages() {
    return numSentMessages;
  }

  public void setNumSentMessages(long numSentMessages) {
    if (numSentMessages < 0) {
      this.numSentMessages = 0;
    } else {
      this.numSentMessages = numSentMessages;
    }
  }

  public long getNumReceivedMessages() {
    return numReceivedMessages;
  }

  public void setNumReceivedMessages(long numReceivedMessages) {
    this.numReceivedMessages = numReceivedMessages;
  }

  public void setParameters(String app, String sessionId) {
    setParameters(AppData.valueOf(app), sessionId);
  }

  public void setParameters(AppData appData, String sessionId) {
    this.appData = appData;
    this.sessionId = sessionId;
  }
}
