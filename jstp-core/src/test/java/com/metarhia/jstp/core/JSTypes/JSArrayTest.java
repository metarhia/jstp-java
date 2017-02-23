package com.metarhia.jstp.core.JSTypes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JSArrayTest {

  private JSArray jsArray;

  @Before
  public void setUp() {
    jsArray = new JSArray();
  }

  @After
  public void tearDown() {
    jsArray.clear();
  }

  @Test
  public void add() throws Exception {
    final JSBool value = new JSBool(true);
    jsArray.add(value);
    assertTrue(jsArray.contains(value));
    jsArray.clear();
  }

  @Test
  public void add1() throws Exception {
    String value = "random data";
    jsArray.add(value);
    assertTrue(jsArray.contains(value));
    jsArray.clear();
  }

  @Test
  public void add2() throws Exception {
    Double value = 666.666;
    jsArray.add(value);
    assertTrue(jsArray.contains(value));
    jsArray.clear();
  }

  @Test
  public void add3() throws Exception {
    boolean value = false;
    jsArray.add(value);
    assertTrue(jsArray.contains(value));
    jsArray.clear();
  }

  @Test
  public void get() throws Exception {
    jsArray.clear();
    JSString value = new JSString("yyyyy");
    jsArray.add(value);

    assertEquals(value, jsArray.get(0));
    jsArray.clear();
  }
}