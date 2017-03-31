package com.metarhia.jstp.core.JSTypes;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JSBoolTest {

  @Test
  public void getValue() throws Exception {
    JSBool bool = new JSBool(true);

    assertTrue(bool.getValue());
  }

  @Test
  public void setValue() throws Exception {
    JSBool bool = new JSBool(true);
    bool.setValue(false);

    assertFalse(bool.getValue());
  }
}