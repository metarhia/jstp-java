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
