package com.metarhia.jstp.core.Tokens;

import com.metarhia.jstp.core.JSParsingException;
import com.metarhia.jstp.core.JSTypes.JSUndefined;
import com.metarhia.jstp.core.Utils;
import java.io.Serializable;
import java.lang.reflect.Field;

public class Tokenizer implements Serializable {

  private static final String BOOL_TRUE_STR = Boolean.TRUE.toString();
  private static final String BOOL_FALSE_STR = Boolean.FALSE.toString();
  private static final String UNDEFINED_STR = JSUndefined.get().toString();
  private static final String NULL_STR = "null";
  private static final String INFINITY_STR = String.valueOf(Double.POSITIVE_INFINITY);
  private static final String NAN_STR = String.valueOf(Double.NaN);

  private static final CharRange NUMBER_RANGE = new CharRange(0x30, 0x39);
  private static final CharRange LETTER_UPPER_RANGE = new CharRange(0x41, 0x5a);
  private static final CharRange LETTER_LOWER_RANGE = new CharRange(0x61, 0x7a);
  public static final int MAX_INT_VALUE_LENGTH = String.valueOf(Integer.MAX_VALUE).length();

  private final FloatMatchRange floatMatchRange = new FloatMatchRange();

  private final StringBuilder cachedBuilder = new StringBuilder(30);

  private static Field valueReflection = null;

  static {
    try {
      valueReflection = String.class.getDeclaredField("value");
      valueReflection.setAccessible(true);
    } catch (NoSuchFieldException e) {
      valueReflection = null;
    }
  }

  private int length;
  private Number number;
  private String str;
  private char[] input;
  private int index;
  private int prevIndex;
  private Token lastToken;
  private char ch;

  public Tokenizer(String input) {
    setInput(input);
  }

  public Token next() throws JSParsingException {
    if (index >= length) {
      return lastToken = Token.NONE;
    }

    do {
      ch = input[index];
    } while (++index < length && Character.isWhitespace(ch));

    prevIndex = index - 1;

    if (ch == '/' && input[index] == '/') {
      index = indexOf('\n', index) + 1;
      ch = input[index];
    }

    switch (ch) {
      case '[':
        return lastToken = Token.SQ_OPEN;
      case ']':
        return lastToken = Token.SQ_CLOSE;
      case '{':
        return lastToken = Token.CURLY_OPEN;
      case '}':
        return lastToken = Token.CURLY_CLOSE;
      case ':':
        return lastToken = Token.COLON;
      case ',':
        return lastToken = Token.COMMA;
    }

    if (ch == 0x22 || ch == 0x27) { // double and single quotes
//        if (ch == '"' || ch == '\'') {

      int lastIndex = indexOf(ch, index);
      while (lastIndex != -1
          && input[lastIndex - 1] == '\\') {
        lastIndex = indexOf(ch, lastIndex + 1);
      }
      if (lastIndex == -1) {
        throw new JSParsingException(index - 1, "Unmatched quote");
      }

      str = Utils.unescapeString(input, index, lastIndex, cachedBuilder);
      index = lastIndex + 1; // skip quote
      return lastToken = Token.STRING;
    }

    if (isIdentifierStart(ch)) {
      // identifier
      int pastLastIndex = getPastLastIdentifierIndex(input, index - 1);
      str = Utils.unescapeString(input, index - 1, pastLastIndex, cachedBuilder);
      index = pastLastIndex;

      if (str.equals(NULL_STR)) {
        return lastToken = Token.NULL;
      } else if (str.equals(UNDEFINED_STR)) {
        return lastToken = Token.UNDEFINED;
      } else if (str.equals(BOOL_TRUE_STR)) {
        return lastToken = Token.TRUE;
      } else if (str.equals(BOOL_FALSE_STR)) {
        return lastToken = Token.FALSE;
      } else if (str.equals(NAN_STR)) {
        number = Double.NaN;
        return lastToken = Token.NUMBER;
      } else if (str.equals(INFINITY_STR)) {
        number = Double.POSITIVE_INFINITY;
        return lastToken = Token.NUMBER;
      }
      return lastToken = Token.KEY;
    }
    if (isFloatingNumberStart(ch)) {
      boolean inf = false;
      try {
        int matchingCount;
        if (index < length && input[index] == 'I') {
          // Infinity with + or - sign hence add 1 but index is at the next pos
          // so remove 1, because just 'Infinity' was checked out
          // in the previous case
          matchingCount = INFINITY_STR.length();
          inf = true;
        } else {
          matchingCount = getMatchingCount(input, index, floatMatchRange);
        }
        // add one because index is at the next pos (after ch)
        str = new String(input, index - 1, matchingCount + 1);
        // check for number to pass Infinity to else branch
        if (!inf && str.indexOf('.') == -1) {
          if (str.length() < MAX_INT_VALUE_LENGTH) {
            number = Integer.parseInt(str);
          } else {
            Long n = Long.parseLong(str);
            if (n >= Integer.MIN_VALUE && n <= Integer.MAX_VALUE) {
              number = n.intValue();
            } else {
              number = n;
            }
          }
        } else {
          number = Double.parseDouble(str);
        }
        index += matchingCount;
        return lastToken = Token.NUMBER;
      } catch (NumberFormatException e) {
        String errMessage = "Number parsing failed";
        if (inf) {
          errMessage = "Invalid format: expected Infinity, +Infinity, -Infinity";
        }
        throw new JSParsingException(index - 1, errMessage, e);
      }
    }

    return lastToken = Token.NONE;
  }

  private int indexOf(char ch, int from) {
    return Utils.indexOf(input, ch, from, length);
  }

  private int getPastLastIdentifierIndex(char[] input, int index) throws JSParsingException {
    while (index < length) {
      char ch = input[index];
      if (ch == '\\'
          && index + 1 < length
          && input[index + 1] == 'u') {
        int nextIndex = -1;
        if (index + 2 < length
            && input[index + 2] == '{') {
          nextIndex = indexOf('}', index + 3);
        } else if (index + 5 < length) {
          nextIndex = index + 4;
        }
        if (nextIndex >= 0) {
          index = nextIndex;
        } else {
          throw new JSParsingException(index, "Invalid unicode escape character sequence");
        }
      } else if (!isIdentifierPart(ch)) {
        break;
      }
      ++index;
    }
    return index;
  }

  private int getPastLastIndex(char[] input, int index, MatchRange matcher) {
    while (index < length && matcher.matches(input[index])) {
      ++index;
    }
    return index;
  }

  private int getMatchingCount(char[] input, int index, MatchRange matcher) {
    int count = 0;
    while (index < length && matcher.matches(input[index])) {
      ++index;
      ++count;
    }
    return count;
  }

  private static boolean isIdentifierStart(char ch) {
    return Character.isUnicodeIdentifierStart(ch) || ch == '_' || ch == '$' || ch == '\\';
  }

  private static boolean isIdentifierPart(char ch) {
    return Character.isUnicodeIdentifierPart(ch) || ch == '_' || ch == '$';
  }

  public static boolean isLetter(char ch) {
    return LETTER_LOWER_RANGE.contains(ch) || LETTER_UPPER_RANGE.contains(ch);
  }

  public static boolean isNumber(char ch) {
    return NUMBER_RANGE.contains(ch);
  }

  public static boolean isFloatingNumberStart(char ch) {
    return isNumber(ch) || ch == '.' || ch == '+' || ch == '-';
  }

  public static boolean isFloatingNumber(char ch) {
    return isNumber(ch) || ch == '.';
  }

  public String getStr() {
    if (str == null && lastToken == Token.NUMBER) {
      str = String.valueOf(number);
    }
    return str;
  }

  public Number getNumber() {
    return number;
  }

  public Token getLastToken() {
    return lastToken;
  }

  public int getPrevIndex() {
    return prevIndex;
  }

  public void setInput(String input) {
    reset();
    try {
      if (valueReflection != null) {
        this.input = (char[]) valueReflection.get(input);
      } else {
        this.input = input.toCharArray();
      }
      this.length = input.length();
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Cannot access string internal array", e);
    }
  }

  private void reset() {
    number = 0;
    str = null;
    input = null;
    index = 0;
    prevIndex = 0;
    lastToken = null;
    length = 0;
  }

  private static class FloatMatchRange implements MatchRange {

    @Override
    public boolean matches(char ch) {
      return isNumber(ch) || ch == '.';
    }
  }

  public interface MatchRange {

    boolean matches(char ch);
  }
}
