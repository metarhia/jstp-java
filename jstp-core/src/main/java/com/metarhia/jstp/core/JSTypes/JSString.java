package com.metarhia.jstp.core.JSTypes;

import com.metarhia.jstp.core.Utils;

public class JSString implements JSValue {

    private String value;

    public JSString(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public Object getGeneralizedValue() {
        return value;
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
        return "'" + Utils.escapeString(value) + "'";
    }
}
