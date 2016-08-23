package com.metarhia.jstp.Connection;

import com.metarhia.jstp.core.JSTypes.JSArray;
import com.metarhia.jstp.core.JSTypes.JSObject;
import com.metarhia.jstp.core.JSTypes.JSValue;

public class JSTPMessage {

    /**
     * Generated message
     */
    private JSObject message;

    /**
     * Custom user arguments contained in message (added for convenience as there are
     * usually only payload object
     */
    private JSArray args;

    /**
     * Arguments specific for protocol (always contains packageNumber)
     */
    private JSArray protocolArgs;

    /**
     * Number of package corresponding to this message
     */
    private int packageNumber;

    private JSTPMessage() {
        this.message = new JSObject();
        this.args = new JSArray();
    }

    public JSTPMessage(int packageNumber, String type, String argsKey, JSArray args) {
        this(packageNumber, type);

        this.args = args;
        message.put(argsKey, this.args);
    }

    public JSTPMessage(int packageNumber, String type) {
        this(packageNumber, type, null);
    }

    public JSTPMessage(int packageNumber, String type, String argsKey, JSValue... args) {
        this();

        this.packageNumber = packageNumber;

        this.protocolArgs = new JSArray();
        this.protocolArgs.add(this.packageNumber);

        message.put(type, this.protocolArgs);

        if (argsKey != null) {
            this.args.addAll(args);
            message.put(argsKey, this.args);
        }
    }

    public void addProtocolArgs(JSValue... args) {
        this.protocolArgs.addAll(args);
    }

    public void addProtocolArg(String value) {
        this.protocolArgs.add(value);
    }

    public void addProtocolArg(double number) {
        this.protocolArgs.add(number);
    }

    public void addProtocolArg(boolean value) {
        this.protocolArgs.add(value);
    }

    public void addArgs(JSValue... args) {
        this.args.addAll(args);
    }

    public void addArg(String value) {
        this.args.add(value);
    }

    public void addArg(double number) {
        this.args.add(number);
    }

    public void addArg(boolean value) {
        this.args.add(value);
    }

    public void put(String key, JSValue value) {
        message.put(key, value);
    }

    public JSObject getMessage() {
        return message;
    }

    public JSArray getArgs() {
        return args;
    }

    public int getPackageNumber() {
        return packageNumber;
    }

    public void setPackageNumber(int packageNumber) {
        this.packageNumber = packageNumber;

        this.protocolArgs.set(0, this.packageNumber);
    }
}
