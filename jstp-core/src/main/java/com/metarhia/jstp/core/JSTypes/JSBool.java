package com.metarhia.jstp.core.JSTypes;

public class JSBool implements JSValue {

    private boolean value;

    public JSBool(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public Object getGeneralizedValue() {
        return value;
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
