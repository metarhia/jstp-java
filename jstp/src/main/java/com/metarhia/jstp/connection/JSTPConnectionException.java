package com.metarhia.jstp.connection;

/**
 * Created by lundibundi on 8/23/16.
 */
public class JSTPConnectionException extends Exception {
    public JSTPConnectionException() {
    }

    public JSTPConnectionException(String message) {
        super(message);
    }

    public JSTPConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public JSTPConnectionException(Throwable cause) {
        super(cause);
    }

    public JSTPConnectionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
