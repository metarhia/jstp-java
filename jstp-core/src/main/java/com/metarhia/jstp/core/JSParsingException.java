package com.metarhia.jstp.core;

public class JSParsingException extends Exception {

  public JSParsingException(String message) {
    super(message);
  }

  public JSParsingException(int index, String errorMsg) {
    super(String.format("Index: %d, Message: %s", index, errorMsg));
  }

  public JSParsingException(int index, Throwable cause) {
    super(String.format("Index: %d", index), cause);
  }

  public JSParsingException(int index, String errorMsg, Throwable cause) {
    super(String.format("Index: %d, Message: %s", index, errorMsg), cause);
  }

  public JSParsingException(Throwable cause) {
    super(cause);
  }
}
