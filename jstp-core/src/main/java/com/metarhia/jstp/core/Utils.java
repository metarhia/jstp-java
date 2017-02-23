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

  public static String unescapeString(String input) throws ParseException {
    StringBuilder result = new StringBuilder(input.length());
    int backslash = 0;
    int index = input.indexOf('\\');
    while (index >= 0) {
      index++;
      result.append(input.substring(backslash, index - 1));
      final ControlChar controlChar = getControlChar(input, index);
      result.append(controlChar.chars);
      backslash = index + controlChar.size;
      index = input.indexOf('\\', backslash);
    }
    if (backslash < input.length()) {
      result.append(input.substring(backslash, input.length()));
    }
    return result.toString();
  }

  public static ControlChar getControlChar(String input, int start) throws ParseException {
    int codePoint;
    switch (input.charAt(start)) {
      case '"':
        return new ControlChar(new char[]{'"'}, 1);
      case '\'':
        return new ControlChar(new char[]{'\''}, 1);
      case 'b':
        return new ControlChar(new char[]{'\b'}, 1);
      case 'f':
        return new ControlChar(new char[]{'\f'}, 1);
      case 'n':
        return new ControlChar(new char[]{'\n'}, 1);
      case 'r':
        return new ControlChar(new char[]{'\r'}, 1);
      case 't':
        return new ControlChar(new char[]{'\t'}, 1);
      case '0':
        return new ControlChar(new char[]{'\0'}, 1);
      case 'x':
        codePoint = Integer.parseInt(input.substring(start + 1, start + 3), 16);
        return new ControlChar(Character.toChars(codePoint), 3);
      case 'u':
        start++;
        final char character = input.charAt(start);
        if (isHex(character)) {
          codePoint = Integer.parseInt(input.substring(start, start + 4), 16);
          return new ControlChar(Character.toChars(codePoint), 5);
        } else if (character == '{') {
          int i = ++start;
          while (isHex(input.charAt(i))) {
            i++;
          }
          codePoint = Integer.parseInt(input.substring(start, i), 16);
          return new ControlChar(Character.toChars(codePoint), i - start + 3);
        } else {
          throw new ParseException("Invalid Unicode escape sequence", start + 1);
        }
      default:
        return new ControlChar(new char[]{input.charAt(start)}, 1);
    }
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

