package com.metarhia.jstp.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSTypes.IndexedHashMap;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Created by lundibundi on 5/19/17.
 */
class IndexedHashMapTest {

  @Test
  void getByIndex() {
    final JSObject actual = TestUtils.mapOfClass(IndexedHashMap.class,
        "a", 1,
        "b", 2);

    assertEquals(1, (int) actual.getByIndex(0));
    assertEquals(2, (int) actual.getByIndex(1));
  }

  @Test
  void put() {
    final Map<String, Integer> expected =
        TestUtils.mapOfClass(HashMap.class, "a", 1, "b", 2);
    final Map<String, Integer> actual =
        TestUtils.mapOfClass(IndexedHashMap.class, "a", 1, "b", 2);

    assertThat(actual).containsAllEntriesOf(expected);
  }

  @Test
  void putAll() {
    final Map<String, Integer> expected =
        TestUtils.mapOfClass(HashMap.class, "a", 1, "b", 2);
    final Map<String, Integer> actual = new IndexedHashMap<>();

    actual.putAll(expected);

    assertThat(actual).containsAllEntriesOf(expected);
  }

  @Test
  void remove() {
    final IndexedHashMap<Integer> actual = TestUtils.mapOfClass(IndexedHashMap.class,
        "a", 1,
        "b", 2);

    actual.remove("b");

    assertThat(actual.keys()).doesNotContain("b");
    assertThat(actual).doesNotContainKeys("b");
  }

  @Test
  void clear() {
    final IndexedHashMap<Integer> actual = TestUtils.mapOfClass(IndexedHashMap.class,
        "a", 1,
        "b", 2);

    actual.clear();

    assertThat(actual).isEmpty();
    assertThat(actual.keys()).isEmpty();
  }
}
