package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSParsingException;
import com.metarhia.jstp.core.JSSerializer;
import org.mockito.ArgumentMatcher;

class MessageMatcher implements ArgumentMatcher<String> {

  protected JSObject expectedMessage;

  public MessageMatcher(String expectedMessage) throws JSParsingException {
    this(JSParser.<JSObject>parse(expectedMessage));
  }

  public MessageMatcher(JSObject expectedMessage) {
    this.expectedMessage = expectedMessage;
  }

  protected void alterMessage(JSObject another) {
    // ignore
  }

  @Override
  public boolean matches(String message) {
    try {
      JSObject another = JSParser.parse(message);
      alterMessage(another);
      return expectedMessage.equals(another);
    } catch (JSParsingException e) {
      throw new RuntimeException("Parsing of response failed, input: " + message, e);
    }
  }

  @Override
  public String toString() {
    return JSSerializer.stringify(this.expectedMessage);
  }
}
