package com.metarhia.jstp.core.JSTypes;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSInterfaces.JSSerializable;
import com.metarhia.jstp.core.JSSerializer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of Map that doesn't actually implement Map interface
 * internally it just contains all key-value pair in List.
 * Made to improve performance of JSObject as the only operation
 * that is mostly used is {@link JSObject#getByIndex(int)}.
 * This inmplementation doesn't check for key existence in
 * {@link #put(String, Object)} method.
 */
public class ArrayMap<T> implements JSObject<T>, JSSerializable {

  private List<JSEntry<T>> values;

  public ArrayMap() {
    values = new ArrayList<>();
  }

  public ArrayMap(Collection<Map.Entry<String, T>> values) {
    this.values = new ArrayList<>(values.size());
    for (Map.Entry<String, T> entry : values) {
      this.values.add(new JSEntry<>(entry.getKey(), entry.getValue()));
    }
  }

  @Override
  public T getByIndex(int index) {
    return values.get(index).getValue();
  }

  /**
   * Doesn't check {@param key} is present in collection and will
   * add it anyway
   *
   * @param key key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @return always returns null;
   */
  @Override
  public T put(String key, T value) {
    values.add(new JSEntry<>(key, value));
    return null;
  }

  @Override
  public String getKey(int index) {
    return values.get(index).getKey();
  }

  /**
   * Highly inefficient, implemented to comply with interface
   */
  @Override
  public T remove(Object key) {
    for (Iterator<JSEntry<T>> it = values.iterator(); it.hasNext();) {
      JSEntry<T> next = it.next();
      if (next.getKey().equals(key)) {
        T old = next.getValue();
        it.remove();
        return old;
      }
    }
    return null;
  }

  @Override
  public int size() {
    return values.size();
  }

  @Override
  public boolean isEmpty() {
    return values.isEmpty();
  }

  /**
   * Highly inefficient, implemented to comply with interface
   */
  @Override
  public T get(Object key) {
    for (JSEntry<T> JSEntry : values) {
      if (JSEntry.getKey().equals(key)) {
        return JSEntry.getValue();
      }
    }
    return null;
  }

  /**
   * Highly inefficient, implemented to comply with interface
   */
  @Override
  public boolean containsKey(Object key) {
    return get(key) != null;
  }

  /**
   * Highly inefficient, implemented to comply with interface
   */
  @Override
  public boolean containsValue(Object value) {
    for (JSEntry<T> JSEntry : values) {
      if (JSEntry.getValue().equals(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void putAll(Map<? extends String, ? extends T> m) {
    for (Entry<? extends String, ? extends  T> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    values.clear();
  }

  @Override
  public Set<String> keySet() {
    Set<String> keys = new HashSet<>(values.size());
    for (JSEntry<T> JSEntry : values) {
      keys.add(JSEntry.getKey());
    }
    return keys;
  }

  /**
   * Highly inefficient, implemented to comply with interface
   */
  @Override
  public Set<Entry<String, T>> entrySet() {
    Set<Entry<String, T>> entrySet = new HashSet<>();
    entrySet.addAll(this.values);
    return entrySet;
  }

  /**
   * Highly inefficient, implemented to comply with interface
   */
  public Collection<? extends String> keys() {
    List<String> keys = new ArrayList<>(values.size());
    for (JSEntry<T> JSEntry : values) {
      keys.add(JSEntry.getKey());
    }
    return keys;
  }

  @Override
  public Collection<T> values() {
    Collection<T> values = new ArrayList<>(this.values.size());
    for (JSEntry<T> JSEntry : this.values) {
      values.add(JSEntry.getValue());
    }
    return values;
  }

  @Override
  public Collection<? extends Map.Entry<String, T>> entries() {
    return values;
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    return JSSerializer.stringifyIterable(values, builder);
  }
}
