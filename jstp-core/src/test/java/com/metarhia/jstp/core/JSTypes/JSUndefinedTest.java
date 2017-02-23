package com.metarhia.jstp.core.JSTypes;

import static org.junit.Assert.assertSame;

import org.junit.Test;

public class JSUndefinedTest {

  @Test
  public void equals() throws Exception {
    // must exist only one instance
    assertSame(JSUndefined.get(), JSUndefined.get());
  }
}