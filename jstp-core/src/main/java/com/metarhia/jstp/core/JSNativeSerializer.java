package com.metarhia.jstp.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lundibundi on 4/2/17.
 */
public class JSNativeSerializer {

  public static String stringify(Object input) {
    return stringify(input, new StringBuilder(30)).toString();
  }

  public static StringBuilder stringify(Object input, StringBuilder builder) {
    if (input instanceof Map) {
      return stringifyObject((LinkedHashMap<String, ?>) input, builder);
    } else if (input instanceof List) {
      return stringifyArray((List) input, builder);
    } else if (input instanceof String) {
      return builder.append("'")
          .append(Utils.escapeString((String) input))
          .append("'");
    } else if (input instanceof Number) {
      return stringifyNumber((Number) input, builder);
    } else if (input == null) {
      return builder.append("null");
    }
    return builder.append(input);
  }

  public static StringBuilder stringifyNumber(Number value, StringBuilder builder) {
    if (value.doubleValue() == value.longValue()) {
      return builder.append(value.longValue());
    } else {
      return builder.append(value);
    }
  }

  public static StringBuilder stringifyArray(List input, StringBuilder builder) {
    builder.append('[');
    if (input.size() != 0) {
      for (Object value : input) {
        stringify(value, builder)
            .append(',');
      }
      builder.replace(builder.length() - 1, builder.length(), "]");
    } else {
      builder.append(']');
    }
    return builder;
  }

  public static StringBuilder stringifyObject(LinkedHashMap<String, ?> object,
      StringBuilder builder) {
    builder.append('{');
    if (object.size() != 0) {
      for (Map.Entry<String, ?> entry : object.entrySet()) {
        builder.append(entry.getKey())
            .append(':');
        stringify(entry.getValue(), builder);
        builder.append(',');
      }
      builder.replace(builder.length() - 1, builder.length(), "}");
    } else {
      builder.append('}');
    }
    return builder;
  }
}
