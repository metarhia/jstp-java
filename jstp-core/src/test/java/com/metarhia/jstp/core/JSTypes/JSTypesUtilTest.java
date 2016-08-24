package com.metarhia.jstp.core.JSTypes;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by lundibundi on 8/24/16.
 */
public class JSTypesUtilTest {
    @Test
    public void javaToJSNumber() throws Exception {
        final int value = 4;
        JSValue expected = new JSNumber(value);
        JSValue actual = JSTypesUtil.javaToJS(value);

        assertEquals(expected, actual);
    }

    @Test
    public void javaToJSString() throws Exception {
        final String value = "dddf";
        JSValue expected = new JSString(value);
        JSValue actual = JSTypesUtil.javaToJS(value);

        assertEquals(expected, actual);
    }

    @Test
    public void javaToJSArray() throws Exception {
        final List<Object> values = Arrays.<Object>asList("dddf", 3);
        JSArray expected = new JSArray();
        expected.add(new JSString("dddf"));
        expected.add(new JSNumber(3));

        JSValue actual = JSTypesUtil.javaToJS(values);

        assertEquals(expected, actual);
    }
}