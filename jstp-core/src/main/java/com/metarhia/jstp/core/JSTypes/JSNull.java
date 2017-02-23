package com.metarhia.jstp.core.JSTypes;

/**
 * Created by lida on 21.04.16.
 */
public final class JSNull implements JSValue {

  private static JSNull instance;

  private JSNull() {
  }

  public static JSNull get() {
    if (instance == null) {
      instance = new JSNull();
    }
    return instance;
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this
        || obj instanceof JSNull;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    return "null";
  }

  @Override
  public Object getGeneralizedValue() {
    return this;
  }
}
