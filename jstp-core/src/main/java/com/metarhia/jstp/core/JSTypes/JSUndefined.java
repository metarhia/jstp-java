package com.metarhia.jstp.core.JSTypes;

/**
 * Created by lida on 19.04.16.
 */
public class JSUndefined implements JSValue {

    private static JSUndefined instance;
    private JSUndefined() {}

    public static JSUndefined get() {
        if(instance == null) {
            instance = new JSUndefined();
        }
        return instance;
    }

    @Override
    public Object getGeneralizedValue() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
                || obj instanceof JSUndefined;
    }

    @Override
    public String toString() {
        return "undefined";
    }
}
