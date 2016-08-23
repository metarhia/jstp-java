package com.metarhia.jstp.core.JSTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by lida on 21.04.16.
 */
public class JSArray implements JSValue {

    private List<JSValue> values;

    public JSArray() {
        values = new ArrayList<>();
    }

    public void add(JSValue value) {
        values.add(value);
    }

    public void add(String value) {
        values.add(new JSString(value));
    }

    public void add(double number) {
        values.add(new JSNumber(number));
    }

    public void add(boolean value) {
        values.add(new JSBool(value));
    }

    public JSValue get(int index) {
        return values.get(index);
    }

    public JSValue set(int i, JSValue value) {
        return values.set(i, value);
    }

    public JSValue set(int i, String value) {
        return values.set(i, new JSString(value));
    }

    public JSValue set(int i, double value) {
        return values.set(i, new JSNumber(value));
    }

    public JSValue set(int i, boolean value) {
        return values.set(i, new JSBool(value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JSArray jsArray = (JSArray) o;

        return values != null ? values.equals(jsArray.values) : jsArray.values == null;
    }

    @Override
    public int hashCode() {
        return values != null ? values.hashCode() : 0;
    }

    public List<JSValue> getValue() {
        return values;
    }

    @Override
    public Object getGeneralizedValue() {
        return values;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[ ");
        if (values.size() != 0) {
            for (JSValue value : values) {
                builder.append(value)
                        .append(", ");
            }
            builder.replace(builder.length() - 2, builder.length(), " ]");
        } else {
            builder.append("]");
        }
        return builder.toString();
    }

    public void addAll(JSValue[] values) {
        this.values.addAll(Arrays.asList(values));
    }

    public void addAll(Collection<JSValue> values) {
        this.values.addAll(values);
    }
}
