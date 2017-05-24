package com.metarhia.jstp.connection;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Contains information about session
 */
public class SessionData implements Serializable {

  private String appName;

  private String sessionID;

  private AtomicLong messageCounter;

  private long numSentMessages;

  private long numReceivedMessages;

  public SessionData() {
    this(null);
  }

  public SessionData(String appName) {
    this(appName, 0);
  }

  public SessionData(String appName, long messageCounter) {
    this.appName = appName;
    this.messageCounter = new AtomicLong(messageCounter);
  }

  public SessionData(String appName, String sessionID,
                     long messageCounter, long numSentMessages, long numReceivedMessages) {
    this(appName, messageCounter);
    this.sessionID = sessionID;
    this.numSentMessages = numSentMessages;
    this.numReceivedMessages = numReceivedMessages;
  }

  public long getAndIncrementMessageCounter() {
    return messageCounter.getAndIncrement();
  }

  public void incrementNumSentMessages() {
    this.numSentMessages++;
  }

  public void incrementNumReceivedMessages() {
    this.numReceivedMessages++;
  }

  public void reset() {
    sessionID = null;
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

    if (numSentMessages != that.numSentMessages) {
      return false;
    }
    if (numReceivedMessages != that.numReceivedMessages) {
      return false;
    }
    if (appName != null ? !appName.equals(that.appName) : that.appName != null) {
      return false;
    }
    if (sessionID != null ? !sessionID.equals(that.sessionID) : that.sessionID != null) {
      return false;
    }
    return messageCounter.get() == that.messageCounter.get();
  }

  @Override
  public int hashCode() {
    int result = appName != null ? appName.hashCode() : 0;
    result = 31 * result + (sessionID != null ? sessionID.hashCode() : 0);
    result = 31 * result + messageCounter.hashCode();
    result = 31 * result + (int) (numSentMessages ^ (numSentMessages >>> 32));
    result = 31 * result + (int) (numReceivedMessages ^ (numReceivedMessages >>> 32));
    return result;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getSessionID() {
    return sessionID;
  }

  public void setSessionID(String sessionID) {
    this.sessionID = sessionID;
  }

  public AtomicLong getMessageCounter() {
    return messageCounter;
  }

  public void setMessageCounter(AtomicLong messageCounter) {
    this.messageCounter = messageCounter;
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
}
