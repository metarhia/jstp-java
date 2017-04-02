package com.metarhia.jstp.core;

import com.metarhia.jstp.core.JSTypes.JSUndefined;
import com.metarhia.jstp.core.Tokens.Token;
import com.metarhia.jstp.core.Tokens.Tokenizer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class JSNativeParser implements Serializable {

  public static final boolean VERBOSE_CHECKING = true;

  private Tokenizer tokenizer;

  public JSNativeParser() {
    tokenizer = new Tokenizer("");
  }

  public JSNativeParser(String input) {
    tokenizer = new Tokenizer(input);
  }

  public static Object parse(String input) throws JSParsingException {
    return new JSNativeParser(input).parse();
  }

  public Object parse() throws JSParsingException {
    return parse(true);
  }

  public Object parse(boolean skip) throws JSParsingException {
    if (skip) {
      tokenizer.next();
    }

    switch (tokenizer.getLastToken()) {
      case TRUE:
        return true;
      case FALSE:
        return false;
      case STRING:
        return tokenizer.getStr();
      case CURLY_OPEN:
        return parseObject();
      case SQ_OPEN:
        return parseArray();
      case NUMBER:
        return tokenizer.getNumber();
      case NULL:
        return null;
      default:
        return JSUndefined.get();
    }
  }

  public List<Object> parseArray() throws JSParsingException {
    assureToken("Error: expected '[' at the beginning of JSArray", Token.SQ_OPEN);

    List<Object> array = new ArrayList<>();
    while (tokenizer.getLastToken() != Token.SQ_CLOSE
        && tokenizer.next() != Token.SQ_CLOSE) {
      if (tokenizer.getLastToken() == Token.COMMA) {
        array.add(JSUndefined.get());
      } else {
        array.add(parse(false));
        // skip comma
        if (tokenizer.next() != Token.COMMA
            && tokenizer.getLastToken() != Token.SQ_CLOSE) {
          throw new JSParsingException(tokenizer.getPrevIndex(),
              "Expected ',' as separator of array elements");
        }
      }
    }
    return array;
  }

  public LinkedHashMap<String, Object> parseObject() throws JSParsingException {
    assureToken("Expected '{' at the beginning of JSObject", Token.CURLY_OPEN);

    LinkedHashMap<String, Object> hash = new LinkedHashMap<>();
    while (tokenizer.getLastToken() != Token.CURLY_CLOSE
        && tokenizer.next() != Token.CURLY_CLOSE) {
      final KeyValuePair<String, Object> keyValuePair = parseKeyValuePair();
      hash.put(keyValuePair.getKey(), keyValuePair.getValue());
      // skip comma
      if (tokenizer.next() != Token.COMMA && tokenizer.getLastToken() != Token.CURLY_CLOSE) {
        throw new JSParsingException(tokenizer.getPrevIndex(),
            "Expected ',' as key-value pairs separator");
      }
    }
    return hash;
  }

  public KeyValuePair<String, Object> parseKeyValuePair() throws JSParsingException {
    assureToken("Expected valid key", Token.KEY, Token.NUMBER, Token.STRING);

    String key = tokenizer.getStr();

    if (tokenizer.next() != Token.COLON) {
      throw new JSParsingException(tokenizer.getPrevIndex(),
          "Expected ':' as separator of Key and Value");
    }

    Object value = parse(true);

    return new KeyValuePair<>(key, value);
  }

  private void assureToken(String errorMsg, Token... tokens) throws JSParsingException {
    if (!VERBOSE_CHECKING) {
      return;
    }

    boolean assured = assure(tokenizer.getLastToken(), tokens);
    if (!assured) {
      assured = assure(tokenizer.next(), tokens);
      if (!assured) {
        throw new JSParsingException(tokenizer.getPrevIndex(), errorMsg);
      }
    }
  }

  private boolean assure(Token tokenToAssure, Token[] array) {
    for (Token token : array) {
      if (token == tokenToAssure) {
        return true;
      }
    }
    return false;
  }

  public void setInput(String input) {
    tokenizer.setInput(input);
  }

  public static class KeyValuePair<T, F> {

    private T key;
    private F value;

    public KeyValuePair(T key, F value) {
      this.key = key;
      this.value = value;
    }

    public T getKey() {
      return key;
    }

    public void setKey(T key) {
      this.key = key;
    }

    public F getValue() {
      return value;
    }

    public void setValue(F value) {
      this.value = value;
    }
  }
}
