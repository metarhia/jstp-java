package com.metarhia.jstp.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lundibundi on 12/27/16.
 */
public class TestUtils {

  public static <T, F> Map mapOf(Object... keyValuePairs) {
    return TestUtils.<T, F, HashMap>mapOfClass(HashMap.class, keyValuePairs);
  }

  public static <T, F, R extends Map<T, F>> R mapOfClass(Class<R> clazz, Object... keyValuePairs) {
    try {
      R map = clazz.newInstance();
      return mapOfClass(map, keyValuePairs);
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Failed to instantiate map object", e);
    }
  }

  public static <T, F, R extends Map<T, F>> R mapOfClass(R map, Object... keyValuePairs) {
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      if (i + 1 < keyValuePairs.length) {
        T key = (T) keyValuePairs[i];
        F value = (F) keyValuePairs[i + 1];
        map.put(key, value);
      }
    }
    return map;
  }

  public static class TestData<T, F> {

    T input;
    F expected;

    public TestData(T input, F expected) {
      this.input = input;
      this.expected = expected;
    }
  }
}
