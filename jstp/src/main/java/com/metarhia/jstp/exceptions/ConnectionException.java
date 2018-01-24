package com.metarhia.jstp.exceptions;

public class ConnectionException extends RuntimeException {

  private int errorCode;

  public ConnectionException(int errorCode) {
    this.errorCode = errorCode;
  }

  public ConnectionException(int errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ConnectionException(int errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public ConnectionException(int errorCode, Throwable cause) {
    super(cause);
    this.errorCode = errorCode;
  }

  public ConnectionException(int errorCode, String message, Throwable cause,
                             boolean enableSuppression,
                             boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.errorCode = errorCode;
  }

  public int getErrorCode() {
    return errorCode;
  }
}
