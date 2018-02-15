package com.metarhia.jstp.compiler;

public class HandlerProcessorException extends RuntimeException {

  public HandlerProcessorException() {
  }

  public HandlerProcessorException(String message) {
    super(message);
  }

  public HandlerProcessorException(String message, Throwable cause) {
    super(message, cause);
  }

  public HandlerProcessorException(Throwable cause) {
    super(cause);
  }

  public HandlerProcessorException(String message, Throwable cause, boolean enableSuppression,
                                   boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
