package com.metarhia.jstp.core.JSTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lundibundi on 8/24/16.
 */
public class JSTypesUtil {

    /**
     * Supported types: {@link Integer} {@link Double} {@link String} {@link Boolean}
     * {@link List}
     *
     * Wraps java value to be used in JS hierarchy
     *
     * @param value object of java type to be wrapped in JS hierarchy
     *
     * @return appropriate JSValue or JSUndefined
     */
    public static JSValue javaToJS(Object value) {
        if (value instanceof Number) {
            return new JSNumber(((Number) value).doubleValue());
        } else if (value instanceof String) {
            return new JSString((String) value);
        } else if (value instanceof Boolean) {
            return new JSBool((Boolean) value);
        } else if (value instanceof List<?>) {
            return new JSArray((List<Object>) value);
        } else if (value instanceof JSValue) {
            return (JSValue) value;
        }
        return JSUndefined.get();
    }

    public static Object JSToJava(JSValue jsValue) {
        return JSToJava(jsValue, false);
    }

    public static Object JSToJava(JSValue jsValue, boolean forceInt) {
        if (jsValue instanceof JSNumber) {
            final double value = ((JSNumber) jsValue).getValue();
            if (forceInt) {
                return (int) value ;
            } else {
                return value;
            }
        } else if (jsValue instanceof JSString) {
            return ((JSString) jsValue).getValue();
        } else if (jsValue instanceof JSBool) {
            return ((JSBool) jsValue).getValue();
        } else if (jsValue instanceof JSArray) {
            List<Object> value = new ArrayList<>();
            for (JSValue val : ((JSArray) jsValue).getValue()) {
                value.add(JSToJava(val, forceInt));
            }
            return value;
        } else if (jsValue instanceof JSObject) {
            Map<String, Object> value = new HashMap<>();
            for (Map.Entry<String, JSValue> me : ((JSObject) jsValue).getValue().entrySet()) {
                value.put(me.getKey(), JSToJava(me.getValue(), forceInt));
            }
            return value;
        }
        return JSUndefined.get();
    }
}
