package com.metarhia.jstp.core.JSTypes;

/**
 * Created by lida on 21.04.16.
 */
public class JSNumber implements JSValue {

    private double value;

    public JSNumber(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public Object getGeneralizedValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JSNumber jsNumber = (JSNumber) o;

        return Double.compare(jsNumber.value, value) == 0;
    }

    @Override
    public int hashCode() {
        long temp = Double.doubleToLongBits(value);
        return (int) (temp ^ (temp >>> 32));
    }

    @Override
    public String toString() {
        if(value == Math.floor(value)) {
            return String.valueOf((long) value);
        } else {
            return String.valueOf(value);
        }
    }
}
