package com.metarhia.jstp.core.JSTypes;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by lundibundi on 8/24/16.
 */
public class JSTypesUtil {
    /**
     * Supported types: {@link Integer} {@link Double} {@link String} {@link Boolean}
     *                  {@link List}
     *
     * Wraps java value to be used in JS hierarchy
     *
     * @param value object of java type to be wrapped in JS hierarchy
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
        }
        return JSUndefined.get();
    }
}
