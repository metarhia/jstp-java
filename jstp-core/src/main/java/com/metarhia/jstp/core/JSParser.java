package com.metarhia.jstp.core;

import com.metarhia.jstp.core.JSTypes.IndexedHashMap;
import com.metarhia.jstp.core.JSTypes.JSEntry;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSTypes.JSUndefined;
import com.metarhia.jstp.core.Tokens.Token;
import com.metarhia.jstp.core.Tokens.Tokenizer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JSParser implements Serializable {

  public final String DEFAULT_PARSE_ERROR_MSG = "Cannot parse";

  public static <T> T parse(String input) throws JSParsingException {
    return new JSParser(input).parse();
  }

  private Tokenizer tokenizer;

  private Class<? extends JSObject> jsObjectClass = IndexedHashMap.class;

  private Class<? extends List> jsArrayClass = ArrayList.class;

  public JSParser() {
    tokenizer = new Tokenizer("");
  }

  public JSParser(String input) {
    tokenizer = new Tokenizer(input);
  }

  public <T> T parse() throws JSParsingException {
    tokenizer.next();
    return parseInternal();
  }

  public <T> T parseInternal() throws JSParsingException {
    switch (tokenizer.getLastToken()) {
      case TRUE:
        return (T) Boolean.TRUE;
      case FALSE:
        return (T) Boolean.FALSE;
      case STRING:
        return (T) tokenizer.getStr();
      case CURLY_OPEN:
        return (T) parseObjectInternal();
      case SQ_OPEN:
        return (T) parseArrayInternal();
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

  public <T> List<T> parseArray() throws JSParsingException {
    tokenizer.next();
    if (tokenizer.getLastToken() != Token.SQ_OPEN) {
      throw new JSParsingException("Error: expected '[' at the beginning of JSArray");
    }
    return parseArrayInternal();
  }

  public <T> List<T> parseArrayInternal() throws JSParsingException {
    List<T> array;
    try {
      array = jsArrayClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Failed to create instance of JS Array", e);
    }

    while (tokenizer.getLastToken() != Token.SQ_CLOSE
        && tokenizer.next() != Token.SQ_CLOSE) {
      if (tokenizer.getLastToken() == Token.COMMA) {
        array.add((T) JSUndefined.get());
      } else {
        array.add((T) parseInternal());
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

  public <T> JSObject<T> parseObject() throws JSParsingException {
    tokenizer.next();
    if (tokenizer.getLastToken() != Token.CURLY_OPEN) {
      throw new JSParsingException("Expected '{' at the beginning of JSObject");
    }
    return parseObjectInternal();
  }

  private <T> JSObject<T> parseObjectInternal() throws JSParsingException {
    JSObject<T> hash;
    try {
      hash = jsObjectClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Failed to create instance of JS Object", e);
    }

    while (tokenizer.getLastToken() != Token.CURLY_CLOSE
        && tokenizer.next() != Token.CURLY_CLOSE) {
      final Map.Entry<String, T> keyValuePair = parseKeyValuePairInternal();
      hash.put(keyValuePair.getKey(), keyValuePair.getValue());
      // skip comma
      if (tokenizer.next() != Token.COMMA && tokenizer.getLastToken() != Token.CURLY_CLOSE) {
        throw new JSParsingException(tokenizer.getPrevIndex(),
            "Expected ',' as key-value pairs separator");
      }
    }
    return hash;
  }

  public <T> JSEntry<T> parseKeyValuePair() throws JSParsingException {
    tokenizer.next();
    assureToken("Expected valid key",
        new HashSet<>(Arrays.asList(Token.KEY, Token.NUMBER, Token.STRING)));
    return parseKeyValuePairInternal();
  }

  private <T> JSEntry<T> parseKeyValuePairInternal() throws JSParsingException {
    String key = tokenizer.getStr();
    if (key == null) {
      throw new JSParsingException(tokenizer.getPrevIndex(), "Expected valid key");
    }

    if (tokenizer.next() != Token.COLON) {
      throw new JSParsingException(tokenizer.getPrevIndex(),
          "Expected ':' as separator of Key and Value");
    }

    T value;
    try {
      tokenizer.next();
      value = parseInternal();
    } catch (JSParsingException e) {
      if (!e.getErrMessage().equals(DEFAULT_PARSE_ERROR_MSG)) {
        // just rethrow when this in not a default error
        throw e;
      }
      throw new JSParsingException(tokenizer.getPrevIndex(),
          "Expected value after ':' in object");
    }

    return new JSEntry<>(key, value);
  }

  private void assureToken(String errorMsg, Set<Token> tokens) throws JSParsingException {
    if (!tokens.contains(tokenizer.getLastToken())) {
      throw new JSParsingException(tokenizer.getPrevIndex(), errorMsg);
    }
  }

  public void setInput(String input) {
    tokenizer.setInput(input);
  }

  public Class<? extends JSObject> getJsObjectClass() {
    return jsObjectClass;
  }

  public void setJsObjectClass(Class<? extends JSObject> jsObjectClass) {
    this.jsObjectClass = jsObjectClass;
  }

  public Class<? extends List> getJsArrayClass() {
    return jsArrayClass;
  }

  public void setJsArrayClass(Class<? extends List> jsArrayClass) {
    this.jsArrayClass = jsArrayClass;
  }
}
