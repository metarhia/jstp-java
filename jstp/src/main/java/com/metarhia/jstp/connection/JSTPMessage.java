package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSTypes.JSArray;
import com.metarhia.jstp.core.JSTypes.JSObject;
import com.metarhia.jstp.core.JSTypes.JSValue;

public class JSTPMessage {

  /**
   * Generated message
   */
  private JSObject message;

  /**
   * Custom user arguments contained in message (added for convenience as there are
   * usually only payload object
   */
  private JSValue args;

  /**
   * Arguments specific for protocol (always contains packageNumber)
   */
  private JSArray protocolArgs;

  /**
   * Number of package corresponding to this message
   */
  private int packageNumber;

  /**
   * Implementation will by no means invalidate this value, it is the responsibility of the user to
   * keep this Optional field consistent
   */
  private String stringRepresentation;

  private JSTPMessage() {
    this.message = new JSObject();
    this.args = new JSArray();
  }

  public JSTPMessage(int packageNumber, String type) {
    this(packageNumber, type, null, null);
  }

  public JSTPMessage(int packageNumber, String type, String argsKey, JSValue args) {
    this();

    this.packageNumber = packageNumber;

    this.protocolArgs = new JSArray();
    this.protocolArgs.add(this.packageNumber);

    message.put(type, this.protocolArgs);

    if (argsKey != null) {
      this.args = args;
      message.put(argsKey, this.args);
    }
  }

  public void addProtocolArgs(JSValue... args) {
    this.protocolArgs.addAll(args);
  }

  public void addProtocolArg(String value) {
    this.protocolArgs.add(value);
  }

  public void addProtocolArg(double number) {
    this.protocolArgs.add(number);
  }

  public void addProtocolArg(boolean value) {
    this.protocolArgs.add(value);
  }

  public void put(String key, JSValue value) {
    message.put(key, value);
  }

  public JSObject getMessage() {
    return message;
  }

  public JSValue getArgs() {
    return args;
  }

  public int getPackageNumber() {
    return packageNumber;
  }

  public void setPackageNumber(int packageNumber) {
    this.packageNumber = packageNumber;

    this.protocolArgs.set(0, this.packageNumber);
  }

  public String getStringRepresentation() {
    return stringRepresentation;
  }

  public void setStringRepresentation(String stringRepresentation) {
    this.stringRepresentation = stringRepresentation;
  }
}
