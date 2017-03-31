package com.metarhia.jstp.core.JSTypes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


public class JSStringTest {

  @Test
  public void getValue() throws Exception {
    final String value = "another test";
    JSString jsString = new JSString(value);

    assertEquals(value, jsString.getValue());
  }

  @Test
  public void setValue() throws Exception {
    JSString jsString = new JSString("another test");
    final String newValue = "yet another test";
    jsString.setValue(newValue);

    assertEquals(newValue, jsString.getValue());
  }
}