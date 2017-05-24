package com.metarhia.jstp;

import java.nio.charset.Charset;

/**
 * Created by lundibundi on 2/22/17.
 */
public class TestConstants {

  public static final String REMOTE_HOST = "since.tv";
  public static final int REMOTE_PORT = 4000;
  public static final String REMOTE_APP_NAME = "superIn";

  public static final String MOCK_APP_NAME = "nothing";
  public static final String MOCK_HANDSHAKE_REQUEST = "{handshake:[0,'%s']}";
  public static final String MOCK_HANDSHAKE_RESPONSE = "{handshake:[0],ok:'%s'}";

  public static final String MOCK_HANDSHAKE_REQUEST_SESSION = "{handshake:[0,'%s'],session:['%s',%d]}";
  public static final String MOCK_HANDSHAKE_RESPONSE_SESSION = "{handshake:[0],ok:%d}";

  public static final String MOCK_HANDSHAKE_REQUEST_RESTORE = "{handshake:[0],ok:['sessionID',22]}";
  public static final String MOCK_HANDSHAKE_RESTORE = "{handshake:[0],ok:33}";
  public static final int MOCK_HANDSHAKE_RESPONSE_ERR_CODE = 16;
  public static final String MOCK_HANDSHAKE_RESPONSE_ERR = "{handshake:[0],error:["
      + MOCK_HANDSHAKE_RESPONSE_ERR_CODE + "]}";

  public static final String MOCK_CALL = "{call:[%d,'%s'],%s:%s}";
  public static final String MOCK_CALLBACK = "{callback:[%d],%s:%s}";
  public static final String MOCK_EVENT = "{event:[%d,'%s'],%s:%s}";
  public static final String MOCK_INSPECT = "{inspect:[%d,'%s']}";

  public static final Charset UTF_8_CHARSET = Constants.UTF_8_CHARSET;
}
