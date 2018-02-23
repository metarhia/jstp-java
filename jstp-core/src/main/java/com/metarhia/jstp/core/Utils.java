package com.metarhia.jstp.core;

public final class Utils {

  private static final String[] CONTROL_CHARS = {
      "\\u0000", "\\u0001", "\\u0002",
      "\\u0003", "\\u0004", "\\u0005",
      "\\u0006", "\\u0007", "\\u0008",
      "\\u0009", "\\u000a", "\\u000b",
      "\\u000c", "\\u000d", "\\u000e",
      "\\u000f", "\\u0010", "\\u0011",
      "\\u0012", "\\u0013", "\\u0014",
      "\\u0015", "\\u0016", "\\u0017",
      "\\u0018", "\\u0019", "\\u001a",
      "\\u001b", "\\u001c", "\\u001d",
      "\\u001e", "\\u001f"
  };

  private Utils() {
  }

  public static String escapeString(String input) {
    return escapeString(input, new StringBuilder()).toString();
  }

  public static StringBuilder escapeString(String input, StringBuilder builder) {
    builder.ensureCapacity(builder.length() + input.length());
    for (int i = 0; i < input.length(); i++) {
      final char c = input.charAt(i);
      String escaped = getEscapedCharacter(c);
      if (escaped == null) {
        builder.append(c);
      } else {
        builder.append(escaped);
      }
    }
    return builder;
  }

  public static String getEscapedCharacter(char c) {
    switch (c) {
      case '\b':
        return "\\b";
      case '\f':
        return "\\f";
      case '\n':
        return "\\n";
      case '\r':
        return "\\r";
      case '\t':
        return "\\t";
      case '\\':
        return "\\\\";
      case '\'':
        return "\\'";
      default:
        if (c < 0x20) {
          return CONTROL_CHARS[c];
        } else if (c == 0x7f) {
          return "\\u007F";
        } else {
          return null;
        }
    }
  }

  public static String unescapeString(char[] input, int fromIndex, int maxIndex)
      throws JSParsingException {
    return unescapeString(input, fromIndex, maxIndex, new StringBuilder(30));
  }

  public static String unescapeString(char[] input, int fromIndex,
                                      int maxIndex, StringBuilder builder)
      throws JSParsingException {
    int backslash = fromIndex;
    int index = indexOf(input, '\\', backslash, maxIndex);
    if (index < 0) {
      return new String(input, fromIndex, maxIndex - fromIndex);
    }
    builder.setLength(0);
    builder.ensureCapacity(maxIndex - fromIndex);
    while (index >= 0) {
      index++;
      builder.append(input, backslash, index - backslash - 1);
      backslash = index + addControlChar(input, index, builder);
      index = indexOf(input, '\\', backslash, maxIndex);
    }
    if (backslash < maxIndex) {
      builder.append(input, backslash, maxIndex - backslash);
    }
    return builder.toString();
  }

  public static int addControlChar(char[] input, int start, StringBuilder dst)
      throws JSParsingException {
    int codePoint;
    switch (input[start]) {
      case '"':
        dst.append('"');
        return 1;
      case '\'':
        dst.append('\'');
        return 1;
      case 'b':
        dst.append('\b');
        return 1;
      case 'f':
        dst.append('\f');
        return 1;
      case 'n':
        dst.append('\n');
        return 1;
      case 'r':
        dst.append('\r');
        return 1;
      case 't':
        dst.append('\t');
        return 1;
      case '0':
        dst.append('\0');
        return 1;
      case 'x':
        codePoint = Integer.parseInt(new String(input, start + 1, 2), 16);
        dst.append(Character.toChars(codePoint));
        return 3;
      case 'u':
        try {
          start++;
          final char character = input[start];
          if (isHex(character)) {
            codePoint = Integer.parseInt(new String(input, start, 4), 16);
            dst.append(Character.toChars(codePoint));
            return 5;
          } else if (character == '{') {
            int i = ++start;
            while (isHex(input[i])) {
              i++;
            }
            i -= start;
            codePoint = Integer.parseInt(new String(input, start, i), 16);
            dst.append(Character.toChars(codePoint));
            return i + 3;
          } else {
            throw new JSParsingException(start + 1, "Invalid Unicode escape sequence");
          }
        } catch (NumberFormatException e) {
          throw new JSParsingException(start + 1, "Invalid Unicode escape sequence", e);
        }
      default:
        dst.append(input[start]);
        return 1;
    }
  }

  public static double charArrayToDouble(char[] data, int start, int length, int[] end) {
    boolean neg = false;
    if (data[start] == '-') {
      neg = true;
      start++;
    } else if (data[start] == '+') {
      start++;
    }

    long value = 0;
    int i;
    int dotIndex = -1;
    int powerMod = 0;
    for (i = start; i < length; i++) {
      final char ch = data[i];
      if (ch >= '0' && ch <= '9') {
        powerMod++;
        value = value * 10 + (ch - '0');
      } else if (ch == '.' && dotIndex < 0) {
        dotIndex = i;
      } else if (ch == 'e' || ch == 'E') {
        powerMod -= charArrayToInt(data, i + 1, length, end);
        break;
      } else {
        break;
      }
    }
    end[0] = i;
    if (dotIndex < 0) {
      dotIndex = i;
    }
    powerMod -= dotIndex - start;
    double result = value / Math.pow(10, powerMod);
    return neg ? -result : result;
  }

  public static int charArrayToInt(char[] data, int start, int length, int[] end)
      throws NumberFormatException {
    boolean neg = false;
    if (data[start] == '-') {
      neg = true;
      start++;
    } else if (data[start] == '+') {
      start++;
    }
    int result = 0;
    int i;
    for (i = start; i < length; i++) {
      int digit = data[i] - '0';
      if (digit < 0 || digit > 9) {
        return result;
      }
      result = result * 10 + digit;
    }
    end[0] = i;
    return neg ? -result : result;
  }

  public static int indexOf(char[] input, char ch, int from, int max) {
    for (int i = from; i < max; i++) {
      if (input[i] == ch) {
        return i;
      }
    }
    return -1;
  }

  private static boolean isHex(char character) {
    return (character >= '0' && character <= '9')
        || (character >= 'A' && character <= 'F')
        || (character >= 'a' && character <= 'f');
  }
}

