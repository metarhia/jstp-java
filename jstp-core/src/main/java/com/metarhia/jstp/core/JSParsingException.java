package com.metarhia.jstp.core;

public class JSParsingException extends Exception {

  private String errMessage;

  public JSParsingException(String errMessage) {
    super(errMessage);
    this.errMessage = errMessage;
  }

  public JSParsingException(int index, String errMessage) {
    super(String.format("Index: %d, Message: %s", index, errMessage));
    this.errMessage = errMessage;
  }

  public JSParsingException(int index, Throwable cause) {
    super(String.format("Index: %d", index), cause);
  }

  public JSParsingException(int index, String errMessage, Throwable cause) {
    super(String.format("Index: %d, Message: %s", index, errMessage), cause);
    this.errMessage = errMessage;
  }

  public JSParsingException(Throwable cause) {
    super(cause);
  }

  public String getErrMessage() {
    return errMessage;
  }
}
