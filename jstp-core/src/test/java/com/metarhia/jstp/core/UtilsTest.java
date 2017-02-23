package com.metarhia.jstp.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Created by lundibundi on 12/27/16.
 */
public class UtilsTest {

  private static final TestUtils.TestData[] unescapeTestData = new TestUtils.TestData[]{
      new TestUtils.TestData("abc\\nhh\\tfff", "abc\nhh\tfff"),
      new TestUtils.TestData("abc\\n\\tf\\0ff\\u4455ggg\\u0011",
          "abc\n\tf\0ff\u4455ggg\u0011")
  };

  private static final TestUtils.TestData[] escapeTestData = new TestUtils.TestData[]{
      new TestUtils.TestData(";sdlfkgj\nsdflkgj\u0000,''\u6666",
          ";sdlfkgj\\nsdflkgj\\u0000,''\u6666")
  };

  @Test
  public void escapeString() throws Exception {
    for (TestUtils.TestData td : unescapeTestData) {
      String actual = Utils.unescapeString(td.input);
      assertEquals(td.expected, actual);
    }
  }

  @Test
  public void unescapeString() throws Exception {
    for (TestUtils.TestData td : unescapeTestData) {
      String actual = Utils.unescapeString(td.input);
      assertEquals(td.expected, actual);
    }
  }
}