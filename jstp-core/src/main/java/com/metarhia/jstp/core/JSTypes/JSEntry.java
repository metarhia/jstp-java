package com.metarhia.jstp.core.JSTypes;

import java.util.Map;

/**
 * Created by lundibundi on 5/22/17.
 */
public class JSEntry<T> implements Map.Entry<String, T> {

  private String key;
  private T value;

  public JSEntry(String key, T value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public T getValue() {
    return value;
  }

  @Override
  public T setValue(T value) {
    T old = this.value;
    this.value = value;
    return old;
  }
}
