package com.metarhia.jstp.core;

import java.text.ParseException;

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
                else if (c == 0x7f) return "\\u007F";
                else return null;
        }
    }

    public static String unescapeString(String input) throws ParseException {
        StringBuilder result = new StringBuilder(input.length());
        int[] index = new int[]{0};
        int backslash = 0;
        index[0] = input.indexOf('\\');
        while (index[0] >= 0) {
            index[0]++;
            result.append(input.substring(backslash, index[0] - 1))
                    .append(getControlChar(input, index));
            backslash = index[0];
            index[0] = input.indexOf('\\', backslash);
        }
        if (backslash < input.length()) {
            result.append(input.substring(backslash, input.length()));
        }
        return result.toString();
    }

    public static char[] getControlChar(String input, int[] index) throws ParseException {
        int start = index[0];
        int codePoint;
        switch (input.charAt(start)) {
            case '"':
                index[0]++;
                return new char[]{'"'};
            case '\'':
                index[0]++;
                return new char[]{'\''};
            case 'b':
                index[0]++;
                return new char[]{'\b'};
            case 'f':
                index[0]++;
                return new char[]{'\f'};
            case 'n':
                index[0]++;
                return new char[]{'\n'};
            case 'r':
                index[0]++;
                return new char[]{'\r'};
            case 't':
                index[0]++;
                return new char[]{'\t'};
            case '0':
                index[0]++;
                return new char[]{'\0'};
            case 'x': {
                start++;
                index[0] = start + 3;
                break;
            }
            case 'u': {
                start++;
                final char character = input.charAt(start);
                if (isHex(character)) {
                    index[0] = start + 4;
                } else if (character == '{') {
                    int i = ++start;
                    while (isHex(input.charAt(i))) i++;
                    codePoint = Integer.parseInt(input.substring(start, i), 16);
                    index[0] = i + 1;
                    return Character.toChars(codePoint);
                } else {
                    throw new ParseException("Invalid Unicode escape sequence", start + 1);
                }
                break;
            }
            default: {
                return new char[]{input.charAt(start)};
            }
        }
        codePoint = Integer.parseInt(input.substring(start, index[0]), 16);
        return Character.toChars(codePoint);
    }

    private static boolean isHex(char character) {
        return (character >= '0' && character <= '9')
                || (character >= 'A' && character <= 'F')
                || (character >= 'a' && character <= 'f');
    }
}

