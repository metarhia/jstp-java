package com.metarhia.jstp.core;

/**
 * Created by lundibundi on 12/9/16.
 */
public class Utils {

    private static final String[] CONTROL_CHARS = {
        "\\u0000", "\\u0001", "\\u0002",
        "\\u0003", "\\u0004", "\\u0005",
        "\\u0006", "\\u0007", "\\u0008",
        "\\u0009", "\\u000a", "\\u000b",
        "\\u000c", "\\u000d", "\\u000e",
        "\\u000f", "\\u0010", "\\u0011",
        "\\u0012", "\\u0013", "\\u0014",
        "\\u0015", "\\u0016", "\\u0017",
        "\\u0018", "\\u0019", "\\u001a",
        "\\u001b", "\\u001c", "\\u001d",
        "\\u001e", "\\u001f"
    };

    public static String escapeString(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            String escaped = getEscapedCharacter(c);
            if (escaped == null) builder.append(c);
            else builder.append(escaped);
        }
        return builder.toString();
    }

    public static String getEscapedCharacter(char c) {
        switch (c) {
            case '\b': return "\\b";
            case '\f': return "\\f";
            case '\n': return "\\n";
            case '\r': return "\\r";
            case '\t': return "\\t";
            case '\\': return "\\\\";
            case '\'': return "\\'";
            default:
                if (c < 0x20) return CONTROL_CHARS[c];
                else return null;
        }
    }
}
