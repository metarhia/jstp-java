package com.metarhia.jstp.core.JSTypes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Created by lundibundi on 2/26/17.
 */
public class JSRawTest {

  private static final String jsCode = "api.common.getByPath = (\n"
      + "  data, // data - object/hash\n"
      + "  dataPath // string in dot-separated path\n"
      + ") => {\n"
      + "  const path = dataPath.split('.');\n"
      + "  let obj = data;\n"
      + "  let i, len, next;\n"
      + "  for (i = 0, len = path.length; i < len; i++) {\n"
      + "    next = obj[path[i]];\n"
      + "    if (next === undefined || next === null) return next;\n"
      + "    obj = next;\n"
      + "  }\n"
      + "  return obj;\n"
      + "};\n";

  @Test
  public void toStringTest() throws Exception {
    JSRaw jsRaw = new JSRaw(jsCode);
    assertEquals(jsCode, jsRaw.toString());
  }

  @Test
  public void getJsCode() throws Exception {
    JSRaw jsRaw = new JSRaw(jsCode);
    assertEquals(jsCode, jsRaw.getJsCode());
  }

  @Test
  public void setJsCode() throws Exception {
    JSRaw jsRaw = new JSRaw(jsCode);
    final String whatever = "whatever";
    jsRaw.setJsCode(whatever);
    assertEquals(whatever, jsRaw.getJsCode());
  }
}