package com.metarhia.jstp.core.JSTypes;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;


public class JSNullTest {

  @Test
  public void equals() throws Exception {
    // must exist only one instance
    assertSame(JSNull.get(), JSNull.get());
  }

}