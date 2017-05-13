package com.metarhia.jstp.core.Tokens;

import com.metarhia.jstp.core.JSParsingException;
import com.metarhia.jstp.core.JSTypes.JSBool;
import com.metarhia.jstp.core.JSTypes.JSNull;
import com.metarhia.jstp.core.JSTypes.JSUndefined;
import com.metarhia.jstp.core.Utils;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.ParseException;

public class Tokenizer implements Serializable {

  private static final String BOOL_TRUE_STR = new JSBool(true).toString();
  private static final String BOOL_FALSE_STR = new JSBool(false).toString();
  private static final String UNDEFINED_STR = JSUndefined.get().toString();
  private static final String NULL_STR = JSNull.get().toString();

  private static final CharRange NUMBER_RANGE = new CharRange(0x30, 0x39);
  private static final CharRange LETTER_UPPER_RANGE = new CharRange(0x41, 0x5a);
  private static final CharRange LETTER_LOWER_RANGE = new CharRange(0x61, 0x7a);

  private final IdentifierMatchRange IDENTIFIER_MATCHER = new IdentifierMatchRange();

  private final FloatMatchRange floatMatchRange = new FloatMatchRange();

  private final int[] cachedArray = new int[1];

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
  private double number;
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
    prevIndex = index;

    if (index >= length) {
      return lastToken = Token.NONE;
    }

    do {
      ch = input[index];
    } while (++index < length
        && (ch == 0x20 || ch == 0x0a || ch == 0x09)); // space and \n and \t

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

      try {
        str = Utils.unescapeString(input, index, lastIndex);
      } catch (ParseException e) {
        throw new JSParsingException(e);
      }
      index = lastIndex + 1; // skip quote
      return lastToken = Token.STRING;
    }

//        if (ch == '_' || Character.isLetter(ch)) {
    if (ch == 0x5f || isLetter(ch)) { // underscore
      // identifier
      int lastIndex = getPastLastIndex(input, index, IDENTIFIER_MATCHER);
      str = new String(input, index - 1, lastIndex - index + 1);
      index = lastIndex;

      if (str.equals(NULL_STR)) {
        return lastToken = Token.NULL;
      } else if (str.equals(UNDEFINED_STR)) {
        return lastToken = Token.UNDEFINED;
      } else if (str.equals(BOOL_TRUE_STR)) {
        return lastToken = Token.TRUE;
      } else if (str.equals(BOOL_FALSE_STR)) {
        return lastToken = Token.FALSE;
      }
      return lastToken = Token.KEY;
    } else if (isFloatingNumber(ch)) {
//      int lastIndex = getPastLastIndex(input, index, floatMatchRange);
//      str = new String(input, index - 1, lastIndex - index + 1);
//      number = Double.parseDouble(str);
      try {
        str = null;
        number = Utils.charArrayToDouble(input, index - 1, length, cachedArray);
      } catch (NumberFormatException e) {
        throw new JSParsingException(index - 1, e);
      }
      index = cachedArray[0];
      return lastToken = Token.NUMBER;
    }

    return lastToken = Token.NONE;
  }

  private int indexOf(char ch, int from) {
    return Utils.indexOf(input, ch, from, length);
  }

  private int getPastLastIndex(char[] input, int index, MatchRange matcher) {
    while (index < length
        && matcher.matches(input[index])) {
      ++index;
    }
    return index;
  }

  public static boolean isLetter(char ch) {
    return LETTER_LOWER_RANGE.contains(ch) || LETTER_UPPER_RANGE.contains(ch);
  }

  public static boolean isNumber(char ch) {
    return NUMBER_RANGE.contains(ch);
  }

  public static boolean isFloatingNumber(char ch) {
    return isNumber(ch) || ch == '.' || ch == '+' || ch == '-';
  }

  public String getStr() {
    if (str == null && lastToken == Token.NUMBER) {
      str = String.valueOf(number);
    }
    return str;
  }

  public Double getNumber() {
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

  private static class IdentifierMatchRange implements MatchRange {

    @Override
    public boolean matches(char ch) {
      return isLetter(ch) || isNumber(ch) || ch == 0x5f; // '_'
    }
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
