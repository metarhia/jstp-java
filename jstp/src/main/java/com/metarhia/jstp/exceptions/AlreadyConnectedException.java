package com.metarhia.jstp.exceptions;

/**
 * Exception which occures when connection is already established
 */
public class AlreadyConnectedException extends RuntimeException {

  /**
   * Creates new exception instance
   */
  public AlreadyConnectedException() {
  }

  /**
   * @see Exception#Exception(String)
   */
  public AlreadyConnectedException(String message) {
    super(message);
  }

  /**
   * @see Exception#Exception(String, Throwable)
   */
  public AlreadyConnectedException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * @see Exception#Exception(Throwable)
   */
  public AlreadyConnectedException(Throwable cause) {
    super(cause);
  }

  /**
   * @see Exception#Exception(String, Throwable, boolean, boolean)
   */
  public AlreadyConnectedException(String message, Throwable cause, boolean enableSuppression,
                                   boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
