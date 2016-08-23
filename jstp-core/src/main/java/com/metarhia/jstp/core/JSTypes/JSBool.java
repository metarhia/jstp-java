package com.metarhia.jstp.core.JSTypes;

/**
 * Created by lida on 21.04.16.
 */
public class JSBool implements JSValue {

    private boolean value;

    public JSBool(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public Object getGeneralizedValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj instanceof JSBool) {
            JSBool other = (JSBool) obj;
            return value == other.value;
        }
        return false;
    }
}
