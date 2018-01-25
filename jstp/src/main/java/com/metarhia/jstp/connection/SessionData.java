package com.metarhia.jstp.connection;

import java.io.Serializable;
import java.util.Objects;

/**
 * Contains information about session
 */
public class SessionData implements Serializable {

  private String appName;

  private String sessionId;

  private long numSentMessages;

  private long numReceivedMessages;

  public SessionData() {
    this(null);
  }

  public SessionData(String appName) {
    this(appName, null);
  }

  public SessionData(String appName, String sessionId) {
    this(appName, sessionId, 0, 0);
  }

  public SessionData(String appName, String sessionId,
                     long numSentMessages, long numReceivedMessages) {
    this(appName);
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
        Objects.equals(appName, that.appName) &&
        Objects.equals(sessionId, that.sessionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(appName, sessionId, numSentMessages, numReceivedMessages);
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
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
    if (numSentMessages < 0) this.numSentMessages = 0;
    else this.numSentMessages = numSentMessages;
  }

  public long getNumReceivedMessages() {
    return numReceivedMessages;
  }

  public void setNumReceivedMessages(long numReceivedMessages) {
    this.numReceivedMessages = numReceivedMessages;
  }

  public void setParameters(String appName, String sessionId) {
    this.appName = appName;
    if (sessionId != null) {
      this.sessionId = sessionId;
    }
  }
}
