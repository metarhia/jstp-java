package com.metarhia.jstp.core;

import com.metarhia.jstp.core.JSTypes.JSUndefined;
import com.metarhia.jstp.core.Tokens.Token;
import com.metarhia.jstp.core.Tokens.Tokenizer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JSNativeParser implements Serializable {

  public static final boolean VERBOSE_CHECKING = true;

  public final String DEFAULT_PARSE_ERROR_MSG = "Cannot parse";

  private Tokenizer tokenizer;

  private Class<? extends Map> jsObjectClass = IndexedHashMap.class;
  private Class<? extends List> jsArrayClass = ArrayList.class;

  public JSNativeParser() {
    tokenizer = new Tokenizer("");
  }

  public JSNativeParser(String input) {
    tokenizer = new Tokenizer(input);
  }

  public static <T extends Object> T parse(String input) throws JSParsingException {
    return new JSNativeParser(input).parse();
  }

  public <T extends Object> T parse() throws JSParsingException {
    return parse(true);
  }

  public <T extends Object> T parse(boolean skip) throws JSParsingException {
    if (skip) {
      tokenizer.next();
    }

    switch (tokenizer.getLastToken()) {
      case TRUE:
        return (T) Boolean.TRUE;
      case FALSE:
        return (T) Boolean.FALSE;
      case STRING:
        return (T) tokenizer.getStr();
      case CURLY_OPEN:
        return (T) parseObject(false);
      case SQ_OPEN:
        return (T) parseArray(false);
      case NUMBER:
        return (T) tokenizer.getNumber();
      case UNDEFINED:
        return (T) JSUndefined.get();
      case NULL:
        return null;
      case KEY:
        throw new JSParsingException(tokenizer.getPrevIndex(),
            tokenizer.getStr() + " is not defined");
      default:
        throw new JSParsingException(tokenizer.getPrevIndex(), DEFAULT_PARSE_ERROR_MSG);
    }
  }

  public List<Object> parseArray() throws JSParsingException {
    return parseArray(true);
  }

  private List<Object> parseArray(boolean skip) throws JSParsingException {
    if (skip) {
      tokenizer.next();
    }
    assureToken("Error: expected '[' at the beginning of JSArray", Token.SQ_OPEN);

    List<Object> array;
    try {
      array = jsArrayClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Failed to create instance of JS Object", e);
    }

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

  public Map<String, Object> parseObject() throws JSParsingException {
    return parseObject(true);
  }

  private Map<String, Object> parseObject(boolean skip) throws JSParsingException {
    if (skip) {
      tokenizer.next();
    }
    assureToken("Expected '{' at the beginning of JSObject", Token.CURLY_OPEN);

    Map<String, Object> hash;
    try {
      hash = jsObjectClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Failed to create instance of JS Object", e);
    }

    while (tokenizer.getLastToken() != Token.CURLY_CLOSE
        && tokenizer.next() != Token.CURLY_CLOSE) {
      final KeyValuePair<String, Object> keyValuePair = parseKeyValuePair(false);
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
    return parseKeyValuePair(true);
  }

  private KeyValuePair<String, Object> parseKeyValuePair(boolean skip) throws JSParsingException {
    if (skip) {
      tokenizer.next();
    }
    assureToken("Expected valid key", Token.KEY, Token.NUMBER, Token.STRING);

    String key = tokenizer.getStr();

    if (tokenizer.next() != Token.COLON) {
      throw new JSParsingException(tokenizer.getPrevIndex(),
          "Expected ':' as separator of Key and Value");
    }

    Object value;
    try {
      value = parse(true);
    } catch (JSParsingException e) {
      if (!e.getErrMessage().equals(DEFAULT_PARSE_ERROR_MSG)) {
        // just rethrow when this in not a default error
        throw e;
      }
      throw new JSParsingException(tokenizer.getPrevIndex(),
          "Expected value after ':' in object");
    }

    return new KeyValuePair<>(key, value);
  }

  private void assureToken(String errorMsg, Token... tokens) throws JSParsingException {
    if (!VERBOSE_CHECKING) {
      return;
    }

    boolean assured = assure(tokenizer.getLastToken(), tokens);
    if (!assured) {
      throw new JSParsingException(tokenizer.getPrevIndex(), errorMsg);
    }
  }

  private boolean assure(Token tokenToAssure, Token... array) {
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

  public Class<? extends Map> getJsObjectClass() {
    return jsObjectClass;
  }

  public void setJsObjectClass(Class<? extends Map> jsObjectClass) {
    this.jsObjectClass = jsObjectClass;
  }

  public Class<? extends List> getJsArrayClass() {
    return jsArrayClass;
  }

  public void setJsArrayClass(Class<? extends List> jsArrayClass) {
    this.jsArrayClass = jsArrayClass;
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
