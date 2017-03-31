package com.metarhia.jstp.core.JSTypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class JSArrayTest {

  private static final Object[] randomData = new Object[]{
      true, new JSBool(false), "random", 6666.6666, new JSString("whatever")
  };

  private static final int ARRAY_SIZE = 10;

  private Random random;

  private JSArray jsArray;

  public JSArrayTest() {
    random = new Random();
    jsArray = new JSArray(ARRAY_SIZE);
  }

  @Test
  public void constructorCollection() throws Exception {
    final List<Object> list = Arrays.<Object>asList("10", 11, JSNull.get());
    JSArray array = new JSArray(list);

    for (Object o : list) {
      JSValue jsObject = JSTypesUtil.javaToJS(o);
      assertTrue(array.contains(jsObject));
    }
  }

  @Test
  public void constructorObjects() throws Exception {
    final Object[] list = new Object[] {"10", 11, JSNull.get()};
    JSArray array = new JSArray(list);

    for (Object o : list) {
      JSValue jsObject = JSTypesUtil.javaToJS(o);
      assertTrue(array.contains(jsObject));
    }
  }

  @Test
  public void add() throws Exception {
    for (Object expected : randomData) {
      jsArray.add(expected);
      assertTrue(jsArray.contains(expected));
      jsArray.clear();
    }
  }

  @Test
  public void get() throws Exception {
    jsArray.clear();
    JSString value = new JSString("yyyyy");
    jsArray.add(value);

    assertEquals(value, jsArray.get(0));
    jsArray.clear();
  }

  @Test
  public void set() throws Exception {
    // add some elements to ensure capacity
    jsArray.getValue().addAll(
        Arrays.asList(new JSNumber(33), new JSString("rrr"), new JSBool(false))
    );
    for (Object expected : randomData) {
      int pos = random.nextInt(jsArray.size());
      jsArray.set(pos, expected);
      JSValue actual = jsArray.get(pos);
      if (expected instanceof JSValue) {
        assertTrue(actual.equals(expected));
      } else {
        assertTrue(actual.getGeneralizedValue().equals(expected));
      }
    }
    jsArray.clear();
  }

  @Test
  public void equalsNullAndItself() throws Exception {
    assertFalse(jsArray.equals(null));
    assertTrue(jsArray.equals(jsArray), "equals itself");
  }

  @Test
  public void equalsArr() throws Exception {
    JSArray arr = new JSArray();
    jsArray.add(false);
    assertFalse(jsArray.equals(arr), "basic equals");
    jsArray.clear();
    assertTrue(jsArray.equals(arr), "basic equals");
  }


  @Test
  public void toStringTest() throws Exception {
    for (int i = 0; i < 4; i++) {
      StringBuilder expected = new StringBuilder("[");
      for (int j = 0; j < 4; j++) {
        int pos = random.nextInt(randomData.length);
        Object val = randomData[pos];
        JSValue jsVal = JSTypesUtil.javaToJS(val);
        expected.append(jsVal.toString())
            .append(',');
        jsArray.add(val);
      }
      expected.replace(expected.length() - 1, expected.length(), "]");
      assertEquals(expected.toString(), jsArray.toString());
      jsArray.clear();
    }
  }
}