package com.metarhia.jstp.core;

import org.apache.commons.lang3.text.translate.*;

public class Utils {

    /**
     * Modified version of {@link org.apache.commons.lang3.StringEscapeUtils#ESCAPE_ECMASCRIPT}
     */
    public static final CharSequenceTranslator ESCAPE_ECMASCRIPT =
            new AggregateTranslator(
                    new LookupTranslator(
                            new String[][] {
                                    {"'", "\\'"},
                                    {"\"", "\\\""},
                                    {"\\", "\\\\"},
                            }),
                    new LookupTranslator(EntityArrays.JAVA_CTRL_CHARS_ESCAPE()),
                    JavaUnicodeEscaper.between(0x00, 0x1f),
                    JavaUnicodeEscaper.between(0x7f, 0x7f)
            );

    /**
     * {@see org.apache.commons.lang3.StringEscapeUtils#UNESCAPE_JAVA}
     */
    public static final CharSequenceTranslator UNESCAPE_JAVA =
            new AggregateTranslator(
                    new OctalUnescaper(),     // .between('\1', '\377'),
                    new UnicodeUnescaper(),
                    new LookupTranslator(EntityArrays.JAVA_CTRL_CHARS_UNESCAPE()),
                    new LookupTranslator(
                            new String[][] {
                                    {"\\\\", "\\"},
                                    {"\\\"", "\""},
                                    {"\\'", "'"},
                                    {"\\", ""}
                            })
            );

     /**
     * {@see org.apache.commons.lang3.StringEscapeUtils#UNESCAPE_ECMASCRIPT}
     */
    public static final CharSequenceTranslator UNESCAPE_ECMASCRIPT = UNESCAPE_JAVA;

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
            case '\b':
                return "\\b";
            case '\f':
                return "\\f";
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            case '\t':
                return "\\t";
            case '\\':
                return "\\\\";
            case '\'':
                return "\\'";
            default:
                if (c < 0x20) return CONTROL_CHARS[c];
                else return null;
        }
    }

    public static String unescapeEcmaScript(final String input) {
        return UNESCAPE_ECMASCRIPT.translate(input);
    }

    public static String escapeEcmaScript(final String input) {
        return ESCAPE_ECMASCRIPT.translate(input);
    }
}
