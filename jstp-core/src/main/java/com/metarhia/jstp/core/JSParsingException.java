package com.metarhia.jstp.core;

public class JSParsingException extends Exception {

    public JSParsingException(String message) {
        super(message);
    }

    public JSParsingException(int index, String errorMsg) {
        super(String.format("Index: %d, Message: %s", index, errorMsg));
    }
}
