package com.metarhia.jstp.connection;

/**
 * Created by Lida on 14.07.16.
 */
public enum JSCallback {
  OK("ok"),
  ERROR("error");

  private String value;

  JSCallback(String value) {
    this.value = value;
  }

  public String toString() {
    return value;
  }
}
