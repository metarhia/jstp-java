package com.metarhia.jstp.core.JSTypes;

/**
 * Created by lundibundi on 9/2/16.
 */
public class JSRaw implements JSValue {
    private String jsCode;

    public JSRaw(String jsCode) {
        this.jsCode = jsCode;
    }

    @Override
    public Object getGeneralizedValue() {
        return jsCode;
    }

    @Override
    public String toString() {
        return jsCode;
    }

    public String getJsCode() {
        return jsCode;
    }

    public void setJsCode(String jsCode) {
        this.jsCode = jsCode;
    }
}
