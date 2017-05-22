package com.metarhia.jstp;

import com.metarhia.jstp.core.JSNativeParser;
import com.metarhia.jstp.core.JSNativeSerializer;
import com.metarhia.jstp.core.JSParsingException;
import java.io.Serializable;

/**
 * Common JSTP interface
 * Created by lidaamber on 18.06.16.
 */
public final class JSTP implements Serializable {

  private JSTP() {
  }

  public static <T> T parse(String input) throws JSParsingException {
    return JSNativeParser.parse(input);
  }

  public static <T> String stringify(T value) {
    return JSNativeSerializer.stringify(value);
  }
}
