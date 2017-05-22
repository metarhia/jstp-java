package com.metarhia.jstp.core.JSTypes;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import java.util.List;

public final class JSTypesUtil {

  private JSTypesUtil() {
  }

  public static <T> T getFromArray(List<?> array, int... indexes) {
    Object next = array;
    for (int i : indexes) {
      next = ((List<?>) next).get(i);
    }
    return (T) next;
  }

  public static <T> T getFromObject(JSObject<?> object, String... keys) {
    Object next = object;
    for (String key : keys) {
      next = ((JSObject<?>) next).get(key);
    }
    return (T) next;
  }

  /**
   * Iteratively gets values from {@param value} by keys
   *
   * @param value initial value
   * @param keys array of keys to be fetched
   * @param <T> return type
   * @return
   */
  public static <T> T getMixed(Object value, Object... keys) {
    Object next = value;
    for (Object k : keys) {
      if (k instanceof Integer) {
        next = ((List<?>) next).get((Integer) k);
      } else if (k instanceof Double) {
        next = ((JSObject) next).getByIndex(((Double) k).intValue());
      } else if (k instanceof String) {
        next = ((JSObject) next).get(k);
      } else {
        throw new RuntimeException("Unsupported key type");
      }
    }
    return (T) next;
  }
}
