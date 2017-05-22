package com.metarhia.jstp.core.JSTypes;

public final class JSUndefined {

  private static JSUndefined instance;

  private JSUndefined() {
  }

  public static JSUndefined get() {
    if (instance == null) {
      instance = new JSUndefined();
    }
    return instance;
  }

  public static boolean isUndefined(Object obj) {
    return obj == instance
        || obj instanceof JSUndefined;
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this
        || isUndefined(obj);
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    return "undefined";
  }
}
