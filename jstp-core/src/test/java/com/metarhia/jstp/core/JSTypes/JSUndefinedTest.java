package com.metarhia.jstp.core.JSTypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JSUndefinedTest {

  @Test
  public void isUndefined() throws Exception {
    assertTrue(JSUndefined.isUndefined(JSUndefined.get()));
  }

  @Test
  public void toStringTest() throws Exception {
    assertEquals("undefined", JSUndefined.get().toString());
  }

  @Test
  public void equals() throws Exception {
    // must exist only one instance
    assertSame(JSUndefined.get(), JSUndefined.get());
  }
}