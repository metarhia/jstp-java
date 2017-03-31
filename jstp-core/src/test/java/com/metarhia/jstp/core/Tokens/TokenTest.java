package com.metarhia.jstp.core.Tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Created by lundibundi on 2/26/17.
 */
public class TokenTest {

  private static final List<TestData> tokenTestData = Arrays.asList(
      new TestData('[', Token.SQ_OPEN),
      new TestData(']', Token.SQ_CLOSE),
      new TestData('{', Token.CURLY_OPEN),
      new TestData('}', Token.CURLY_CLOSE),
      new TestData(':', Token.COLON),
      new TestData(',', Token.COMMA)
  );

  @Test
  public void fromString() throws Exception {
    for (TestData td : tokenTestData) {
      assertEquals(td.expected, Token.fromString(td.input));
    }
  }

  private static class TestData {

    char input;
    Token expected;

    public TestData(char input, Token expected) {
      this.input = input;
      this.expected = expected;
    }
  }
}