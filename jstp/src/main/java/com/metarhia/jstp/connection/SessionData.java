package com.metarhia.jstp.connection;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Contains information about session
 */
public class SessionData implements Serializable {

  private String appName;

  private String sessionID;

  private AtomicLong packetCounter;

  private long numSentPackets;

  private long numReceivedPackets;

  public SessionData() {
    this(null);
  }

  public SessionData(String appName) {
    this(appName, 0);
  }

  public SessionData(String appName, long packetCounter) {
    this.appName = appName;
    this.packetCounter = new AtomicLong(packetCounter);
  }

  public SessionData(String appName, String sessionID,
      long packetCounter, long numSentPackets, long numReceivedPackets) {
    this(appName, packetCounter);
    this.sessionID = sessionID;
    this.numSentPackets = numSentPackets;
    this.numReceivedPackets = numReceivedPackets;
  }

  public long getAndIncrementPacketCounter() {
    return packetCounter.getAndIncrement();
  }

  public void incrementNumSentPackets() {
    this.numSentPackets++;
  }

  public void incrementNumReceivedPackets() {
    this.numReceivedPackets++;
  }

  public void reset() {
    sessionID = null;
    this.numReceivedPackets = 0;
    this.numSentPackets = 0;
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

    if (numSentPackets != that.numSentPackets) {
      return false;
    }
    if (numReceivedPackets != that.numReceivedPackets) {
      return false;
    }
    if (appName != null ? !appName.equals(that.appName) : that.appName != null) {
      return false;
    }
    if (sessionID != null ? !sessionID.equals(that.sessionID) : that.sessionID != null) {
      return false;
    }
    return packetCounter.get() == that.packetCounter.get();
  }

  @Override
  public int hashCode() {
    int result = appName != null ? appName.hashCode() : 0;
    result = 31 * result + (sessionID != null ? sessionID.hashCode() : 0);
    result = 31 * result + packetCounter.hashCode();
    result = 31 * result + (int) (numSentPackets ^ (numSentPackets >>> 32));
    result = 31 * result + (int) (numReceivedPackets ^ (numReceivedPackets >>> 32));
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

  public AtomicLong getPacketCounter() {
    return packetCounter;
  }

  public void setPacketCounter(AtomicLong packetCounter) {
    this.packetCounter = packetCounter;
  }

  public long getNumSentPackets() {
    return numSentPackets;
  }

  public void setNumSentPackets(long numSentPackets) {
    this.numSentPackets = numSentPackets;
  }

  public long getNumReceivedPackets() {
    return numReceivedPackets;
  }

  public void setNumReceivedPackets(long numReceivedPackets) {
    this.numReceivedPackets = numReceivedPackets;
  }
}
