package com.metarhia.jstp;

import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSParsingException;
import com.metarhia.jstp.core.JSSerializer;
import java.io.Serializable;

/**
 * Common JSTP interface
 */
public final class JSTP implements Serializable {

  private JSTP() {
  }

  /**
   * Wrapper method for {@link JSParser#parse(String)}
   */
  public static <T> T parse(String input) throws JSParsingException {
    return JSParser.parse(input);
  }

  /**
   * Wrapper method for {@link JSSerializer#stringify(Object)}
   */
  public static <T> String stringify(T value) {
    return JSSerializer.stringify(value);
  }
}
