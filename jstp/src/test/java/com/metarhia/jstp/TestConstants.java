package com.metarhia.jstp;

import java.nio.charset.Charset;

/**
 * Created by lundibundi on 2/22/17.
 */
public class TestConstants {

  public static final String MOCK_APP_NAME = "appName";

  public static final String TEMPLATE_HANDSHAKE_REQUEST = "{handshake:[0,'%s']}";
  public static final String ANY_HANDSHAKE_REQUEST = "\\{handshake:\\[0,'.+'\\]\\}";

  public static final String TEMPLATE_HANDSHAKE_VERSION_REQUEST = "{handshake:[0,'%s','%s']}";

  public static final String TEMPLATE_HANDSHAKE_RESPONSE = "{handshake:[0],ok:'%s'}";
  public static final String ANY_HANDSHAKE_RESPONSE = "\\{handshake:\\[0\\],ok:'\\S+'\\}";

  public static final String TEMPLATE_HANDSHAKE_RESTORE_REQUEST = "{handshake:[0,'%s'],session:['%s',%d]}";
  public static final String ANY_HANDSHAKE_RESTORE_REQUEST = "\\{handshake:\\[0,'\\S+'\\],session:\\['\\S+',\\d+\\]\\}";

  public static final String TEMPLATE_HANDSHAKE_RESTORE_RESPONSE = "{handshake:[0],ok:%d}";
  public static final String ANY_HANDSHAKE_RESTORE_RESPONSE = "\\{handshake:\\[0\\],ok:\\d+\\}";

  public static final String MOCK_HANDSHAKE_RESPONSE_ERR = "{handshake:[0],error:[%d]}";

  public static final String TEMPLATE_CALL = "{call:[%d,'%s'],%s:%s}";
  public static final String ANY_CALL = "\\{call:\\[\\d+,'.+'],\\S+:\\[.*\\]\\}";

  public static final String ANY_PING = "\\{ping:\\[\\d+]\\}";
  public static final String ANY_PONG = "\\{pong:\\[\\d+]\\}";

  public static final String TEMPLATE_CALLBACK = "{callback:[%d],%s:%s}";
  public static final String TEMPLATE_EVENT = "{event:[%d,'%s'],%s:%s}";
  public static final String TEMPLATE_INSPECT = "{inspect:[%d,'%s']}";

  public static final Charset UTF_8_CHARSET = Constants.UTF_8_CHARSET;
}
