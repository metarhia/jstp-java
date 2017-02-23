package com.metarhia.jstp.exceptions;

/**
 * Created by lundibundi on 2/22/17.
 */
public class AlreadyConnectedException extends RuntimeException {

  public AlreadyConnectedException() {
  }

  public AlreadyConnectedException(String message) {
    super(message);
  }

  public AlreadyConnectedException(String message, Throwable cause) {
    super(message, cause);
  }

  public AlreadyConnectedException(Throwable cause) {
    super(cause);
  }

  public AlreadyConnectedException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
