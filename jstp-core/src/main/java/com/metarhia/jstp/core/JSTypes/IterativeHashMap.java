package com.metarhia.jstp.core.JSTypes;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Implementation of JSObject that accesses nth key by iterating over
 * {@link LinkedHashMap} iterator n times, will have more efficient
 * {@link Map#put(Object, Object)} {@link Map#remove(Object)} and
 * connected methods as it doesn't have additional array to
 * keep track of key indexes contrary to {@link java.util.IdentityHashMap}.
 *
 * @{see java.util.LinkedHashMap}
 */
public class IterativeHashMap<V> extends LinkedHashMap<String, V> implements JSObject<V> {

  public IterativeHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public IterativeHashMap(int initialCapacity) {
    super(initialCapacity);
  }

  public IterativeHashMap() {
  }

  public IterativeHashMap(Map<? extends String, ? extends V> m) {
    super(m);
  }

  public IterativeHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
    super(initialCapacity, loadFactor, accessOrder);
  }

  @Override
  public V getByIndex(int index) {
    final Entry<String, V> entry = getMapEntry(index);
    return entry == null ? null : entry.getValue();
  }

  private Entry<String, V> getMapEntry(int index) {
    Iterator<Entry<String, V>> it = entrySet().iterator();
    Entry<String, V> entry = null;
    for (int i = 0; i <= index; i++) {
      if (!it.hasNext()) {
        return null;
      }
      entry = it.next();
    }
    return entry;
  }

  @Override
  public String getKey(int index) {
    final Entry<String, V> entry = getMapEntry(index);
    return entry == null ? null : entry.getKey();
  }

  @Override
  public Collection<String> keys() {
    return keySet();
  }

  @Override
  public Collection<? extends Map.Entry<String, V>> entries() {
    return entrySet();
  }
}
