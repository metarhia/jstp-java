package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSParsingException;
import java.util.List;

class MessageMatcherNoCount extends MessageMatcher {

  public MessageMatcherNoCount(String expectedMessage) throws JSParsingException {
    this(JSParser.<JSObject>parse(expectedMessage));
  }

  public MessageMatcherNoCount(JSObject expectedMessage) {
    super(expectedMessage);
    ((List<?>) this.expectedMessage.getByIndex(0)).remove(0);
  }

  @Override
  protected void alterMessage(JSObject another) {
    ((List<?>) another.getByIndex(0)).remove(0);
  }
}
