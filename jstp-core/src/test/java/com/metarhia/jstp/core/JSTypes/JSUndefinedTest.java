package com.metarhia.jstp.core.JSTypes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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