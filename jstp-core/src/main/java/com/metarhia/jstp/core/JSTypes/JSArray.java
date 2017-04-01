package com.metarhia.jstp.core.JSTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by lida on 21.04.16.
 */
public class JSArray implements JSValue {

  private static final int DEFAULT_CAPACITY = 10;

  private List<JSValue> values;

  public JSArray() {
    this(DEFAULT_CAPACITY);
  }

  public JSArray(int initialCapacity) {
    values = new ArrayList<>(initialCapacity);
  }

  /**
   * Parses {@param values} with {@link JSTypesUtil#javaToJS(Object)},
   * see supported types in the link
   */
  public JSArray(Collection<Object> values) {
    this.values = new ArrayList<>(values.size());
    addAll(values);
  }

  /**
   * Parses {@param values} with {@link JSTypesUtil#javaToJS(Object)},
   * see supported types in the link
   */
  public JSArray(Object... values) {
    this.values = new ArrayList<>(values.length);
    addAll((Object[]) values);
  }

  public boolean contains(JSValue value) {
    return values.contains(value);
  }

  /**
   * Parses {@param value} with {@link JSTypesUtil#javaToJS(Object)},
   * see supported types in the link
   */
  public boolean contains(Object value) {
    JSValue jsValue = JSTypesUtil.javaToJS(value);
    return values.contains(jsValue);
  }

  public void clear() {
    values.clear();
  }

  public void add(JSValue value) {
    values.add(value);
  }

  /**
   * Parses {@param value} with {@link JSTypesUtil#javaToJS(Object)},
   * see supported types in the link
   */
  public void add(Object value) {
    values.add(JSTypesUtil.javaToJS(value));
  }

  public void add(double value) {
    values.add(new JSNumber(value));
  }

  public void add(boolean value) {
    values.add(new JSBool(value));
  }

  public JSValue get(int index) {
    return values.get(index);
  }

  public JSValue set(int i, JSValue value) {
    return values.set(i, value);
  }

  public JSValue set(int i, Object value) {
    return values.set(i, JSTypesUtil.javaToJS(value));
  }

  public JSValue set(int i, double value) {
    return set(i, new JSNumber(value));
  }

  public JSValue set(int i, boolean value) {
    return set(i, new JSBool(value));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    JSArray jsArray = (JSArray) o;

    return values != null ? values.equals(jsArray.values) : jsArray.values == null;
  }

  @Override
  public int hashCode() {
    return values != null ? values.hashCode() : 0;
  }

  public List<JSValue> getValue() {
    return values;
  }

  public void addAll(Collection<Object> values) {
    for (Object value : values) {
      add(value);
    }
  }

  public void addAll(Object... values) {
    for (Object value : values) {
      add(value);
    }
  }

  public void addAll(JSValue... values) {
    Collections.addAll(this.values, values);
  }

  public int size() {
    return values.size();
  }

  @Override
  public Object getGeneralizedValue() {
    return values;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("[");
    if (values.size() != 0) {
      for (JSValue value : values) {
        builder.append(value)
            .append(',');
      }
      builder.replace(builder.length() - 1, builder.length(), "]");
    } else {
      builder.append(']');
    }
    return builder.toString();
  }
}
