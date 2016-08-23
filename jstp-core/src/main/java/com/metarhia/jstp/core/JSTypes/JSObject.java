package com.metarhia.jstp.core.JSTypes;

import java.util.*;

/**
 * Created by lida on 19.04.16.
 */
public class JSObject implements JSValue {

    private LinkedHashMap<String, JSValue> values;
    private List<String> orderedKeys;

    public JSObject() {
        values = new LinkedHashMap<>();
        orderedKeys = new ArrayList<>();
    }

    public void put(String key, JSValue value) {
        values.put(key, value);
        orderedKeys.add(key);
    }

    public void put(Entry entry) {
        values.put(entry.key, entry.value);
        orderedKeys.add(entry.key);
    }

    public JSValue get(String key) {
        return values.get(key);
    }

    public JSValue get(int i) {
        if (i >= 0 && i < orderedKeys.size()) {
            return values.get(orderedKeys.get(i));
        }
        return null;
    }

    public boolean containsKey(String key) {
        return values.containsKey(key);
    }

    public Set<Map.Entry<String, JSValue>> orderedEntrySet() {
        return values.entrySet();
    }

    public Collection<JSValue> values() {
        return values.values();
    }

    public LinkedHashMap<String, JSValue> getValue() {
        return values;
    }

    public List<String> getOrderedKeys() {
        return orderedKeys;
    }

    public int indexOf(String key) {
        return orderedKeys.indexOf(key);
    }

    @Override
    public Object getGeneralizedValue() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JSObject object = (JSObject) o;

        return values != null ? values.equals(object.values) : object.values == null;

    }

    @Override
    public int hashCode() {
        return values != null ? values.hashCode() : 0;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("{ ");
        if(values.size() != 0) {
            for (Map.Entry<String, JSValue> entry : values.entrySet()) {
                builder.append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append(", ");
            }
            builder.replace(builder.length() - 2, builder.length(), " }");
        } else {
            builder.append("}");
        }
        return builder.toString();
    }

    public static class Entry {
        public String key;
        public JSValue value;

        public Entry(String key, JSValue value) {
            this.key = key;
            this.value = value;
        }
    }
}
