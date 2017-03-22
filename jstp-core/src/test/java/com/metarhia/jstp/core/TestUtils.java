package com.metarhia.jstp.core;

/**
 * Created by lundibundi on 12/27/16.
 */
public class TestUtils {

  public static class TestData <T, F> {

    T input;
    F expected;

    public TestData(T input, F expected) {
      this.input = input;
      this.expected = expected;
    }
  }
}
