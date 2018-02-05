package com.metarhia.jstp.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.metarhia.jstp.core.TestUtils.TestData;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Created by lundibundi on 4/2/17.
 */
class JSSerializerTest {

  private static final ArrayList<TestData<String, String>> stringifyTestData =
      new ArrayList<>(Arrays.asList(
          new TestData<>("{}", "{}"),
          new TestData<>("[]", "[]"),
          new TestData<>("'abv\\\"gggg\\\"dd'", "'abv\"gggg\"dd'"),
          new TestData<>("'abv\"gggg\"dd'", "'abv\"gggg\"dd'"),
          new TestData<>("['outer', ['inner']]", "[\'outer\',[\'inner\']]"),
          new TestData<>("\'\\u{1F49A}ttt\\u{1F49B}\'", "'💚ttt💛'"),
          new TestData<>("'\\x20'", "' '"),
          new TestData<>("13", "13"),
          new TestData<>("42.1", "42.1"),
          new TestData<>(".1", "0.1")
      ));

  static {
    final TestData[] parseTestData = JSParserTest.parseTestData;
    // 9 index is the first Data that must be reversible
    for (int i = 9; i < parseTestData.length; i++) {
      String input = (String) parseTestData[i].input;
      stringifyTestData.add(new TestData<>(input, input));
    }
  }

  private JSParser parser;

  public JSSerializerTest() {
    parser = new JSParser();
  }

  @Test
  public void stringifyTest() throws Exception {
    for (TestData<String, String> td : stringifyTestData) {
      parser.setInput(td.input);
      Object parsed = parser.parse();
      String actual = JSSerializer.stringify(parsed);
      assertEquals(td.expected, actual, "Failed to parse->stringify: " + td.input);
    }
  }
}
