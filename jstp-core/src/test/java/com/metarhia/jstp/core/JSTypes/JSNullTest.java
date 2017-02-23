package com.metarhia.jstp.core.JSTypes;

import static org.junit.Assert.assertSame;

import org.junit.Test;

public class JSNullTest {

  @Test
  public void equals() throws Exception {
    // must exist only one instance
    assertSame(JSNull.get(), JSNull.get());
  }

}