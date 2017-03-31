package com.metarhia.jstp.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.metarhia.jstp.core.TestUtils.TestData;
import org.junit.jupiter.api.Test;

/**
 * Created by lundibundi on 12/27/16.
 */
public class UtilsTest {

  private static final TestUtils.TestData[] unescapeTestData = new TestUtils.TestData[]{
      new TestUtils.TestData<>("abc\\nhh\\tfff", "abc\nhh\tfff"),
      new TestUtils.TestData<>("abc\\n\\tf\\0ff\\u4455ggg\\u0011",
          "abc\n\tf\0ff\u4455ggg\u0011")
  };

  private static final TestUtils.TestData[] escapeTestData = new TestUtils.TestData[]{
      new TestUtils.TestData<>(";sdlfkgj\ns\"dfl\"kgj\u0000,''\u6666",
          ";sdlfkgj\\ns\"dfl\"kgj\\u0000,\\'\\'\u6666")
  };

  private static final TestUtils.TestData[] doubleTestData = new TestUtils.TestData[]{
      new TestData<>("123", 123.0),
      new TestData<>(".123", 0.123),
      new TestData<>("1.23", 1.23),
      new TestData<>("1", 1.0),
      new TestData<>("-1", -1.0),
      new TestData<>("-112.56", -112.56),
      new TestData<>("123.", 123.0),
      new TestData<>("1.000000000000000001", 1.000000000000000001),
      new TestData<>("1.3e-10", 1.3e-10),
      new TestData<>("-1.3e-10", -1.3e-10),
      new TestData<>("1e100", 1e100),
      new TestData<>("1e-3", 1e-3),
      new TestData<>(".1E-20", .1e-20)
  };

  @Test
  public void escapeString() throws Exception {
    for (TestData<String, String> td : escapeTestData) {
      String actual = Utils.escapeString(td.input);
      assertEquals(td.expected, actual, "Input: " + td.input);
    }
  }

  @Test
  public void unescapeString() throws Exception {
    for (TestData<String, String> td : unescapeTestData) {
      String actual = Utils.unescapeString(td.input.toCharArray(), 0, td.input.length());
      assertEquals(td.expected, actual);
    }
  }

  @Test
  public void charArrayToDouble() throws Exception {
    for (TestUtils.TestData<String, Double> td : doubleTestData) {
      double actual = Utils.charArrayToDouble(
          td.input.toCharArray(), 0, td.input.length(), new int[1]);
      assertEquals(td.expected, actual, 0.000000000000000000000001);
    }
  }
}