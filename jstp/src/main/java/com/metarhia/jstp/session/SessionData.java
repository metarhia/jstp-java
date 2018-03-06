package com.metarhia.jstp.session;

import com.metarhia.jstp.connection.AppData;
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

  /**
   * Creates new empty session data instance
   */
  public SessionData() {
    this(new AppData());
  }

  /**
   * Creates new session data instance for specified application {@param app}
   *
   * @param app application
   */
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

  /**
   * Creates new session data instance for application {@param appData} with session
   * identifier {@param sessionID}, number of sent messages {@param numSentMessages} and
   * number of received messages {@param numReceivedMessages}
   *
   * @param appData             application
   * @param sessionId           session identifier
   * @param numSentMessages     number of sent messages
   * @param numReceivedMessages number of received messages
   */
  public SessionData(AppData appData, String sessionId,
                     long numSentMessages, long numReceivedMessages) {
    this.appData = appData != null ? appData : new AppData();
    this.sessionId = sessionId;
    this.numSentMessages = numSentMessages;
    this.numReceivedMessages = numReceivedMessages;
  }

  /**
   * Increments sent messages counter
   */
  public void incrementNumSentMessages() {
    this.numSentMessages++;
  }

  /**
   * Increments received messages counter
   */
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

  /**
   * Gets session identifier
   *
   * @return session identifier
   */
  public String getSessionId() {
    return sessionId;
  }

  /**
   * Sets session identifier to {@param sessionId}
   *
   * @param sessionId session identifier
   */
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  /**
   * Gets sent messages counter
   *
   * @return sent messages counter
   */
  public long getNumSentMessages() {
    return numSentMessages;
  }

  /**
   * Sets sent messages counter to {@param numSentMessages} if {@param numSentMessages} is positive
   * otherwise sets it to 0
   *
   * @param numSentMessages sent messages counter
   */
  public void setNumSentMessages(long numSentMessages) {
    if (numSentMessages < 0) {
      this.numSentMessages = 0;
    } else {
      this.numSentMessages = numSentMessages;
    }
  }

  /**
   * Get received messages counter
   *
   * @return received messages counter
   */
  public long getNumReceivedMessages() {
    return numReceivedMessages;
  }

  /**
   * Set received messages counter
   *
   * @param numReceivedMessages received messages counter
   */
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
