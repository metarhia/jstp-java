package com.metarhia.jstp.core;

import java.text.ParseException;

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
    StringBuilder builder = new StringBuilder(input.length());
    for (int i = 0; i < input.length(); i++) {
      final char c = input.charAt(i);
      String escaped = getEscapedCharacter(c);
      if (escaped == null) {
        builder.append(c);
      } else {
        builder.append(escaped);
      }
    }
    return builder.toString();
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
      throws ParseException {
    StringBuilder result = new StringBuilder(maxIndex - fromIndex);
    int backslash = fromIndex;
    int index = indexOf(input, '\\', backslash, maxIndex);
    while (index >= 0) {
      index++;
      result.append(input, backslash, index - backslash - 1);
      backslash = index + addControlChar(input, index, result);
      index = indexOf(input, '\\', backslash, maxIndex);
    }
    if (backslash < maxIndex) {
      result.append(input, backslash, maxIndex - backslash);
    }
    return result.toString();
  }

  public static int addControlChar(char[] input, int start, StringBuilder dst)
      throws ParseException {
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
          throw new ParseException("Invalid Unicode escape sequence", start + 1);
        }
      default:
        dst.append(input[start]);
        return 1;
    }
  }

  public static int charArrayToInt(char[] data, int start, int end) throws NumberFormatException {
    int result = 0;
    for (int i = start; i < end; i++) {
      int digit = (int) data[i] - (int) '0';
      if ((digit < 0) || (digit > 9)) {
        throw new NumberFormatException();
      }
      result *= 10;
      result += digit;
    }
    return result;
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

  private static class ControlChar {

    char[] chars;
    int size;

    public ControlChar(char[] chars, int size) {
      this.chars = chars;
      this.size = size;
    }
  }
}

