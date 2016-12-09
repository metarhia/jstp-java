package com.metarhia.jstp;

import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSParsingException;
import com.metarhia.jstp.core.JSTypes.JSObject;
import com.metarhia.jstp.core.JSTypes.JSValue;

import java.io.Serializable;

/**
 * Common JSTP interface
 * Created by lidaamber on 18.06.16.
 */
public class JSTP implements Serializable {

    public static JSObject parse(String data) throws JSParsingException {
        JSParser parser = new JSParser(data);
        return parser.parseObject();
    }

    public static String stringify(JSValue value) {
        return value.toString();
    }
}
