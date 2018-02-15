package com.metarhia.jstp.core;

public class JSParsingException extends Exception {

  private int errorOffset;
  
  private String errMessage;

  public JSParsingException(String errMessage) {
    super(errMessage);
    this.errMessage = errMessage;
  }

  public JSParsingException(int errorOffset, String errMessage) {
    super(String.format("Index: %d, Message: %s", errorOffset, errMessage));
    this.errMessage = errMessage;
    this.errorOffset = errorOffset;
  }

  public JSParsingException(int errorOffset, Throwable cause) {
    super(String.format("Index: %d", errorOffset), cause);
    this.errorOffset = errorOffset;
  }

  public JSParsingException(int errorOffset, String errMessage, Throwable cause) {
    super(String.format("Index: %d, Message: %s", errorOffset, errMessage), cause);
    this.errMessage = errMessage;
    this.errorOffset = errorOffset;
  }

  public JSParsingException(Throwable cause) {
    super(cause);
  }

  public int getErrorOffset() {
    return errorOffset;
  }

  public String getErrMessage() {
    return errMessage;
  }
}
