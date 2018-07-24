package com.metarhia.jstp.connection;

public enum ConnectionError {
  APP_NOT_FOUND(10),
  AUTH_FAILED(11),
  INTERFACE_NOT_FOUND(12),
  INTERFACE_INCOMPATIBLE(13),
  METHOD_NOT_FOUND(14),
  NOT_A_SERVER(15),
  INTERNAL_API_ERROR(16),
  INVALID_SIGNATURE(17),

  // Local Errors
  CALLBACK_LOST(-1);

  private int errorCode;

  ConnectionError(int errorCode) {
    this.errorCode = errorCode;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public static ConnectionError from(int errorCode) {
    for (ConnectionError ce : ConnectionError.values()) {
      if (ce.errorCode == errorCode) {
        return ce;
      }
    }
    return null;
  }
}
