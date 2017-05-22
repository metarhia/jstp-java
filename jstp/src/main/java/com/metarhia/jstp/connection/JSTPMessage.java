package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSTypes.IndexedHashMap;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import java.util.ArrayList;
import java.util.List;

public class JSTPMessage {

  /**
   * Generated message
   */
  private JSObject<Object> message;

  /**
   * Arguments specific for protocol (always contains packageNumber)
   */
  private List<Object> protocolArgs;

  /**
   * Number of package corresponding to this message
   */
  private long packageNumber;

  /**
   * Implementation will by no means invalidate this value, it is the responsibility of the user to
   * keep this Optional field consistent
   */
  private String stringRepresentation;

  private JSTPMessage() {
    this.message = new IndexedHashMap<>(2);
  }

  public JSTPMessage(long packageNumber, String type) {
    this();

    this.packageNumber = packageNumber;

    this.protocolArgs = new ArrayList<>(2);
    this.protocolArgs.add(this.packageNumber);

    message.put(type, this.protocolArgs);
  }

  public JSTPMessage addProtocolArg(Object value) {
    this.protocolArgs.add(value);
    return this;
  }

  public JSTPMessage putArg(String key, Object value) {
    message.put(key, value);
    return this;
  }

  public JSTPMessage setPackageNumber(int packageNumber) {
    this.packageNumber = packageNumber;
    this.protocolArgs.set(0, this.packageNumber);
    return this;
  }

  public JSObject<Object> getMessage() {
    return message;
  }

  public long getPackageNumber() {
    return packageNumber;
  }

  public String getStringRepresentation() {
    return stringRepresentation;
  }

  public void setStringRepresentation(String stringRepresentation) {
    this.stringRepresentation = stringRepresentation;
  }
}
