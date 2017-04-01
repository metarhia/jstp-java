package com.metarhia.jstp.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lundibundi on 12/27/16.
 */
public class TestUtils {

  public static <T, F> Map mapOf(Object... keyValuePairs) {
    final HashMap<T, F> map = new HashMap<>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      if (i + 1 < keyValuePairs.length) {
        map.put((T) keyValuePairs[i], (F) keyValuePairs[i + 1]);
      }
    }
    return map;
  }

  public static class TestData <T, F> {

    T input;
    F expected;

    public TestData(T input, F expected) {
      this.input = input;
      this.expected = expected;
    }
  }
}
