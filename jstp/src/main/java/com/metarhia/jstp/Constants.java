package com.metarhia.jstp;

import java.nio.charset.Charset;

/**
 * JSTP Constants
 */
public class Constants {

  /**
   * Message separator
   */
  public static final char SEPARATOR = '\0';

  private static final String PACKAGE_PREFIX = "com.metarhia.jstp";

  public static final String KEY_SESSION = PACKAGE_PREFIX + ".SESSION";

  public static final String UTF_8_CHARSET_NAME = "UTF-8";

  public static final Charset UTF_8_CHARSET = Charset.forName(UTF_8_CHARSET_NAME);
}
