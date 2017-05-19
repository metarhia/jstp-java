package com.metarhia.jstp.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Created by lundibundi on 5/19/17.
 */
class IndexedHashMapTest {

  @Test
  void getByIndex() {
    final IndexedHashMap actual = TestUtils.mapOfClass(IndexedHashMap.class,
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
  void set() {
    final IndexedHashMap<String, Integer> actual = TestUtils.mapOfClass(IndexedHashMap.class,
        "a", 1,
        "b", 2,
        "c", 3,
        "d", 4);

    actual.set(1, "c", 13);

    assertEquals("c", actual.getKey(1));
    assertEquals(13, (int) actual.getByIndex(1));

    assertEquals("d", actual.getKey(2));
    assertEquals(4, (int) actual.getByIndex(2));
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
    final IndexedHashMap<String, Integer> actual = TestUtils.mapOfClass(IndexedHashMap.class,
        "a", 1,
        "b", 2);

    actual.remove("b");

    assertThat(actual.getKeys()).doesNotContain("b");
    assertThat(actual).doesNotContainKeys("b");
  }

  @Test
  void clear() {
    final IndexedHashMap<String, Integer> actual = TestUtils.mapOfClass(IndexedHashMap.class,
        "a", 1,
        "b", 2);

    actual.clear();

    assertThat(actual).isEmpty();
    assertThat(actual.getKeys()).isEmpty();
  }
}
