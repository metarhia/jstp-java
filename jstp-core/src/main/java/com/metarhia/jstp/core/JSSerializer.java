package com.metarhia.jstp.core;

import com.metarhia.jstp.core.JSInterfaces.JSSerializable;
import com.metarhia.jstp.core.JSTypes.JSUndefined;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

/**
 * Created by lundibundi on 4/2/17.
 */
public class JSSerializer {

  public static String stringify(Object input) {
    return stringify(input, new StringBuilder(30)).toString();
  }

  public static StringBuilder stringify(Object input, StringBuilder builder) {
    if (input instanceof JSSerializable) {
      return ((JSSerializable) input).stringify(builder);
    } else if (input instanceof LinkedHashMap) {
      return stringifyIterable(((LinkedHashMap) input).entrySet(), builder);
    } else if (input instanceof List || input instanceof Queue) {
      return stringifyArray((Collection) input, builder);
    } else if (input instanceof String) {
      builder.append("'");
      Utils.escapeString((String) input, builder);
      builder.append("'");
      return builder;
    } else if (input instanceof Number) {
      return stringifyNumber((Number) input, builder);
    } else if (input instanceof Boolean) {
      return stringifyBool((Boolean) input, builder);
    } else if (input == null) {
      return builder.append("null");
    }
//  } else if (input instanceof JSUndefined) {
    return builder.append(JSUndefined.get().toString());
  }

  public static StringBuilder stringifyNumber(Number value, StringBuilder builder) {
    if (value.doubleValue() == value.longValue()) {
      return builder.append(value.longValue());
    }
    return builder.append(value);
  }

  public static StringBuilder stringifyArray(Collection input, StringBuilder builder) {
    builder.append('[');
    if (input.size() != 0) {
      for (Object value : input) {
        stringify(value, builder).append(',');
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

  public static <T extends Entry<String, ?>> StringBuilder stringifyIterable(
      Collection<T> object,
      StringBuilder builder) {
    builder.append('{');
    if (!object.isEmpty()) {
      for (Map.Entry<String, ?> entry : object) {
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
