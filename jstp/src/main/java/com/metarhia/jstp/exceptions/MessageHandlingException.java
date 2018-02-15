package com.metarhia.jstp.exceptions;

public class MessageHandlingException extends RuntimeException {

  public MessageHandlingException() {
  }

  public MessageHandlingException(String message) {
    super(message);
  }

  public MessageHandlingException(String message, Throwable cause) {
    super(message, cause);
  }

  public MessageHandlingException(Throwable cause) {
    super(cause);
  }

  public MessageHandlingException(String message, Throwable cause, boolean enableSuppression,
                                  boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
