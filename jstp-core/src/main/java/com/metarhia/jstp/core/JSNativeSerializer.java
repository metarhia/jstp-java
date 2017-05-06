package com.metarhia.jstp.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Created by lundibundi on 4/2/17.
 */
public class JSNativeSerializer {

  public static String stringify(Object input) {
    return stringify(input, new StringBuilder(30)).toString();
  }

  public static StringBuilder stringify(Object input, StringBuilder builder) {
    if (input instanceof Map) {
      return stringifyObject((Map<String, ?>) input, builder);
    } else if (input instanceof List || input instanceof Queue) {
      return stringifyArray((Collection) input, builder);
    } else if (input instanceof String) {
      return builder.append("'")
          .append(Utils.escapeString((String) input))
          .append("'");
    } else if (input instanceof Number) {
      return stringifyNumber((Number) input, builder);
    } else if (input instanceof Boolean) {
      return stringifyBool((Boolean) input, builder);
    } else if (input == null) {
      return builder.append("null");
    }
    return builder.append("undefined");
  }

  public static StringBuilder stringifyNumber(Number value, StringBuilder builder) {
    if (value.doubleValue() == value.longValue()) {
      return builder.append(value.longValue());
    } else {
      return builder.append(value);
    }
  }

  public static StringBuilder stringifyArray(Collection input, StringBuilder builder) {
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

  public static StringBuilder stringifyBool(Boolean value, StringBuilder builder) {
    return builder.append(value);
  }

  public static StringBuilder stringifyObject(Map<String, ?> object,
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
