package com.metarhia.jstp.core.Tokens;

public enum Token {
  TRUE,
  FALSE,
  NULL,
  UNDEFINED,
  NUMBER,
  KEY,
  STRING,
  //    SPACE,
  NONE,
  QUOTES_OPEN,
  QUOTES_CLOSE,
  CURLY_OPEN('{'),
  CURLY_CLOSE('}'),
  SQ_OPEN('['),
  SQ_CLOSE(']'),
  COLON(':'),
  COMMA(',');

  private char ch;

  Token(char c) {
    ch = c;
  }

  Token() {
    ch = '0';
  }

  /**
   * Deduces token from given char
   * <p>
   * Warning: tokens that do not have their own character are the same
   * so don't try to use this function for them
   *
   * @param ch character to match
   * @return Token that corresponds to {@param ch} or null if no Token matches
   */
  public static Token fromString(char ch) {
    for (Token token : Token.values()) {
      if (token.ch == ch) {
        return token;
      }
    }
    return null;
  }

  public Character getCh() {
    return ch;
  }
}
