package com.metarhia.jstp.core.JSInterfaces;

import java.util.Collection;
import java.util.Map;

public interface JSObject<V> extends Map<String, V> {

  V getByIndex(int index);

  String getKey(int index);

  Collection<? extends String> keys();

  Collection<? extends Map.Entry<String, V>> entries();
}
