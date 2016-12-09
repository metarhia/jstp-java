package com.metarhia.jstp.core.JSTypes;

public class JSUndefined implements JSValue {

    private static JSUndefined instance;

    private JSUndefined() {
    }

    public static JSUndefined get() {
        if (instance == null) {
            instance = new JSUndefined();
        }
        return instance;
    }

    public static boolean isUndefined(Object obj) {
        return obj == instance
                || obj instanceof JSUndefined;
    }

    @Override
    public Object getGeneralizedValue() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
                || isUndefined(obj);
    }

    @Override
    public String toString() {
        return "undefined";
    }
}
