package com.metarhia.jstp.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.metarhia.jstp.core.JSTypes.JSUndefined;
import com.metarhia.jstp.core.TestUtils.TestData;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

/**
 * Created by lundibundi on 4/2/17.
 */
class JSSerializerTest {

  private static final List<TestData<String, String>> parseStringifyTestData = new ArrayList<>(
      Arrays.asList(
          new TestData<>("{}", "{}"),
          new TestData<>("[]", "[]"),
          new TestData<>("'abv\\\"gggg\\\"dd'", "'abv\"gggg\"dd'"),
          new TestData<>("'abv\"gggg\"dd'", "'abv\"gggg\"dd'"),
          new TestData<>("['outer', ['inner']]", "[\'outer\',[\'inner\']]"),
          new TestData<>("\'\\u{1F49A}ttt\\u{1F49B}\'", "'💚ttt💛'"),
          new TestData<>("{a: 1}", "{a:1}"),
          new TestData<>("{13: 1}", "{13:1}"),
          new TestData<>("{null: 1}", "{null:1}"),
          new TestData<>("{undefined: 1}", "{undefined:1}"),
          new TestData<>("{a: 1, b: 2.0, c: '5555'}", "{a:1,b:2.0,c:'5555'}"),
          new TestData<>("'\\x20'", "' '"),
          new TestData<>("13", "13"),
          new TestData<>("42.1", "42.1"),
          new TestData<>(".1", "0.1")));

  static {
    final TestData[] parseTestData = JSParserTest.parseTestData;
    // 9 index is the first Data that must be reversible
    for (int i = 9; i < parseTestData.length; i++) {
      String input = (String) parseTestData[i].input;
      parseStringifyTestData.add(new TestData<>(input, input));
    }
  }

  private static final List<TestData<Map, List<Map.Entry>>> mapsTestData = new ArrayList<>(
      Arrays.asList(
          new TestData<Map, List<Map.Entry>>(
              Collections.singletonMap("a", 13),
              Arrays.<Map.Entry>asList(new SimpleEntry<>("a", 13))
          )));

  static {
    // add more complex maps
    List<MapTestEntry> elements = Arrays.<MapTestEntry>asList(
        new MapTestEntry<>("abc", 42),
        new MapTestEntry<>("111", Arrays.asList(13)),
        new MapTestEntry<>(42, "42", "abc"),
        new MapTestEntry<>(13.0, "13.0", "cba"),
        new MapTestEntry<>(true, "true", 42),
        new MapTestEntry<>(false, "false", 24),
        new MapTestEntry<>(Arrays.asList(1, 3), "1,3", 113),
        new MapTestEntry<>(JSUndefined.get(), JSUndefined.get().toString(), 42)
                                                             );
    List<Map.Entry> elementEntries = new ArrayList<>();
    Map hashMap = new HashMap();
    Map concurrentHashMap = new ConcurrentHashMap();
    for (MapTestEntry me : elements) {
      hashMap.put(me.key, me.value);
      concurrentHashMap.put(me.key, me.value);
      elementEntries.add(new SimpleEntry<>(me.keyString, me.value));
    }

    mapsTestData.add(new TestData<>(hashMap, elementEntries));
    mapsTestData.add(new TestData<>(concurrentHashMap, elementEntries));
  }


  private JSParser parser;

  public JSSerializerTest() {
    parser = new JSParser();
  }

  @Test
  public void parseStringifyTest() throws Exception {
    for (TestData<String, String> td : parseStringifyTestData) {
      parser.setInput(td.input);
      Object parsed = parser.parse();
      String actual = JSSerializer.stringify(parsed);
      assertEquals(td.expected, actual, "Failed to parse->stringify: " + td.input);
    }
  }

  @Test
  public void stringifyMapsTest() throws Exception {
    for (TestData<Map, List<Map.Entry>> td : mapsTestData) {
      try {
        String actual = JSSerializer.stringify(td.input);
        Map actualParsed = JSParser.parse(actual);
        assertThat(actualParsed).contains(td.expected.toArray(new Map.Entry[]{}))
            .describedAs("Failed map serialization for:\n" + td.input);
      } catch (Exception e) {
        fail("Failed map serialization for:\n" + td.input, e);
      }
    }
  }

  private static class MapTestEntry<T, F> {

    T key;
    String keyString;
    F value;

    public MapTestEntry(T key, F value) {
      this(key, String.valueOf(key), value);
    }

    public MapTestEntry(T key, String keyString, F value) {
      this.key = key;
      this.keyString = keyString;
      this.value = value;
    }
  }
}
