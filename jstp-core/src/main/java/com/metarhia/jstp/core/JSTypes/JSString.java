package com.metarhia.jstp.core.JSTypes;

/**
 * Created by lida on 21.04.16.
 */
public class JSString implements JSValue {

    private String value;

    public JSString(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public Object getGeneralizedValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JSString jsString = (JSString) o;

        return value != null ? value.equals(jsString.value) : jsString.value == null;
    }

    @Override
    public String toString() {
        return "'" + value + "'";
    }
}
