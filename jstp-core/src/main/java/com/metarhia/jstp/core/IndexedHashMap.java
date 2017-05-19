package com.metarhia.jstp.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lundibundi on 5/19/17.
 */
public class IndexedHashMap<K, V> extends HashMap<K, V> {

  private List<K> keys;

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

  public IndexedHashMap(Map<? extends K, ? extends V> m) {
    super(m);
    keys = new ArrayList<>(m.keySet());
  }

  /**
   * Returns value from the map by the index'th key
   *
   * @param index index in keys array
   *
   * @return mapping for key found by {@param index} or null
   *
   * @throws IndexOutOfBoundsException if the index is out of range of keys array (<tt>index &lt; 0
   * || index &gt;= size()</tt>)
   */
  public V getByIndex(int index) {
    return get(keys.get(index));
  }

  public K getKey(int index) {
    return keys.get(index);
  }

  /**
   * Puts value into hash map then adds it to the keys at the
   * specified index if and only if key was not present in map,
   * otherwise index of the key is not changed.
   *
   * {@see HashMap}
   *
   * @param index index to add key at
   */
  public V put(int index, K key, V value) {
    final V prev = super.put(key, value);
    if (prev == null) {
      keys.add(index, key);
    }
    return prev;
  }

  /**
   * Adds value to the map and sets the key at the specified index,
   * if there was a key under that index removes it from the map.
   */
  public V set(int index, K key, V value) {
    final V prev = super.put(key, value);
    keys.remove(key);
    final K prevKey = keys.set(index, key);
    if (prevKey != null) {
      super.remove(prevKey);
    }
    return prev;
  }

  @Override
  public V put(K key, V value) {
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
  public void putAll(Map<? extends K, ? extends V> m) {
    for (K key : m.keySet()) {
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

  public List<K> getKeys() {
    return keys;
  }
}
