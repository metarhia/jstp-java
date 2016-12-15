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

    /**
     * Parses {@param values} with {@link JSTypesUtil#javaToJS(Object)},
     * see supported types in the link
     */
    public JSArray(Collection<Object> values) {
        this.values = new ArrayList<>(values.size());
        for (Object value : values) {
            JSValue jsValue = JSTypesUtil.javaToJS(value);
            this.values.add(jsValue);
        }
    }

    /**
     * Parses {@param values} with {@link JSTypesUtil#javaToJS(Object)},
     * see supported types in the link
     */
    public JSArray(Object[] values) {
        this.values = new ArrayList<>(values.length);
        for (Object value : values) {
            JSValue jsValue = JSTypesUtil.javaToJS(value);
            this.values.add(jsValue);
        }
    }

    public boolean contains(JSValue value) {
        return values.contains(value);
    }

    /**
     * Parses {@param value} with {@link JSTypesUtil#javaToJS(Object)},
     * see supported types in the link
     */
    public boolean contains(Object value) {
        JSValue jsValue = JSTypesUtil.javaToJS(value);
        return values.contains(jsValue);
    }

    public void clear() {
        values.clear();
    }

    public void add(JSValue value) {
        values.add(value);
    }

    public void add(String value) {
        values.add(new JSString(value));
    }

    public void add(double value) {
        values.add(new JSNumber(value));
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

    public void addAll(JSValue[] values) {
        this.values.addAll(Arrays.asList(values));
    }

    public void addAll(Collection<JSValue> values) {
        this.values.addAll(values);
    }

    public int size() {
        return values.size();
    }

    @Override
    public Object getGeneralizedValue() {
        return values;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        if (values.size() != 0) {
            for (JSValue value : values) {
                builder.append(value)
                        .append(",");
            }
            builder.replace(builder.length() - 1, builder.length(), "]");
        } else {
            builder.append("]");
        }
        return builder.toString();
    }
}
