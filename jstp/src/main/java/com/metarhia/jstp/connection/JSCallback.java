package com.metarhia.jstp.connection;

/**
 * Callback status
 */
public enum JSCallback {
  OK("ok"),
  ERROR("error");

  private String value;

  JSCallback(String value) {
    this.value = value;
  }

  public static JSCallback fromString(String name) {
    for (JSCallback t : JSCallback.values()) {
      if (t.value.equals(name)) {
        return t;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return value;
  }
}
