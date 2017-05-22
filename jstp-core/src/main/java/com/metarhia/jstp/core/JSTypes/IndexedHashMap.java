package com.metarhia.jstp.core.JSTypes;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lundibundi on 5/19/17.
 */
public class IndexedHashMap<V> extends LinkedHashMap<String, V> implements JSObject<V> {

  private List<String> keys;

  public IndexedHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
    keys = new ArrayList<>(initialCapacity);
  }

  public IndexedHashMap(int initialCapacity) {
    super(initialCapacity);
    keys = new ArrayList<>(initialCapacity);
  }

  public IndexedHashMap() {
    super();
    keys = new ArrayList<>();
  }

  public IndexedHashMap(Map<String, ? extends V> m) {
    super(m);
    keys = new ArrayList<>(m.keySet());
  }

  /**
   * Returns value from the map by the index'th key
   *
   * @param index index in keys array
   *
   * @return mapping for key found by {@param index} or null if
   *         there is no such mapping or index out of bounds
   */
  @Override
  public V getByIndex(int index) {
    if (index >= keys.size()) {
      return null;
    }
    return get(keys.get(index));
  }

  @Override
  public String getKey(int index) {
    return keys.get(index);
  }

  @Override
  public V put(String key, V value) {
    final V prev = super.put(key, value);
    if (prev == null) {
      keys.add(key);
    }
    return prev;
  }

  /**
   * Very inefficient implementation
   */
  @Override
  public void putAll(Map<? extends String, ? extends V> m) {
    for (String key : m.keySet()) {
      if (!containsKey(key)) {
        keys.add(key);
      }
    }
    super.putAll(m);
  }

  @Override
  public V remove(Object key) {
    final V prev = super.remove(key);
    if (prev != null) {
      keys.remove(key);
    }
    return prev;
  }

  @Override
  public void clear() {
    super.clear();
    keys.clear();
  }

  @Override
  public Collection<String> keys() {
    return keys;
  }

  @Override
  public Collection<? extends Map.Entry<String, V>> entries() {
    return entrySet();
  }
}
