package com.metarhia.jstp.core.Tokens;

import com.metarhia.jstp.core.JSParsingException;
import com.metarhia.jstp.core.JSTypes.JSBool;
import com.metarhia.jstp.core.JSTypes.JSNull;
import com.metarhia.jstp.core.JSTypes.JSUndefined;
import com.metarhia.jstp.core.Utils;

import java.text.ParseException;

public class Tokenizer {
    private static final String BOOL_TRUE_STR = new JSBool(true).toString();
    private static final String BOOL_FALSE_STR = new JSBool(false).toString();
    private static final String UNDEFINED_STR = JSUndefined.get().toString();
    private static final String NULL_STR = JSNull.get().toString();

    private static final CharRange numberRange = new CharRange(0x30, 0x39);
    private static final CharRange letterUpperRange = new CharRange(0x41, 0x5a);
    private static final CharRange letterLowerRange = new CharRange(0x61, 0x7a);
    private final int length;
    private Double number;
    private String str;
    private String input;
    private int index;
    private int prevIndex;
    private Token lastToken;

    public Tokenizer(String input) {
        number = null;
        str = null;
        this.input = input;
        length = input.length();
        index = 0;
        prevIndex = 0;
        lastToken = null;
    }

    private static int getPastLastIndex(String input, int index) {
        return getPastLastIndex(input, index, new MatchRange() {
            @Override
            public boolean matches(char ch) {
                return ch == 0x5f || isLetter(ch) || isNumber(ch);
            }
        });
    }

    private static int getPastLastIndex(String input, int index, MatchRange matcher) {
        while (index < input.length()
                && matcher.matches(input.charAt(index))) {
            ++index;
        }
        return index;
    }

    public static boolean isLetter(char ch) {
        return letterLowerRange.contains(ch) || letterUpperRange.contains(ch);
    }

    public static boolean isNumber(char ch) {
        return numberRange.contains(ch);
    }

    public static boolean isFloatingNumber(char ch) {
        return isNumber(ch) || ch == '.' || ch == '+' || ch == '-';
    }

    public Token next() throws JSParsingException {
        prevIndex = index;

        // reset variables
        str = null;
        number = null;

        char ch = 0;

        if (index >= length) return lastToken = Token.NONE;

        do {
            ch = input.charAt(index);
        } while (++index < length
                && (ch == 0x20 || ch == 0x0a || ch == 0x09)); // space and \n and \t

        if (ch == '/' && input.charAt(index) == '/') {
            index = input.indexOf('\n', index) + 1;
            ch = input.charAt(index);
        }
//        if (index >= input.length()) return lastToken = Token.NONE;

        switch (ch) {
            case '[':
                return lastToken = Token.SQ_OPEN;
            case ']':
                return lastToken = Token.SQ_CLOSE;
            case '{':
                return lastToken = Token.CURLY_OPEN;
            case '}':
                return lastToken = Token.CURLY_CLOSE;
            case ':':
                return lastToken = Token.COLON;
            case ',':
                return lastToken = Token.COMMA;
        }

        if (ch == 0x22 || ch == 0x27) { // double and single quotes
//        if (ch == '"' || ch == '\'') {

            if (index >= length) {
                throw new JSParsingException(index - 1, "No closing quote '" + ch + "'");
            }

            int lastIndex = input.indexOf(ch, index);
            while (lastIndex < length
                    && lastIndex != -1
                    && input.charAt(lastIndex - 1) == '\\') {
                lastIndex = input.indexOf(ch, lastIndex + 1);
            }
            if (lastIndex == -1) {
                throw new JSParsingException(index - 1, "No closing quote '" + ch + "'");
            }

            str = input.substring(index, lastIndex);
            try {
                str = Utils.unescapeString(str);
            } catch (ParseException e) {
                throw new JSParsingException(e);
            }
            index = lastIndex + 1; // skip quote
            return lastToken = Token.STRING;
        }

//        if (ch == '_' || Character.isLetter(ch)) {
        if (ch == 0x5f || isLetter(ch)) { // underscore
            // identifier
            int lastIndex = getPastLastIndex(input, index);
            str = input.substring(index - 1, lastIndex);
            index = lastIndex;

            if (str.equals(NULL_STR)) {
                return lastToken = Token.NULL;
            } else if (str.equals(UNDEFINED_STR)) {
                return lastToken = Token.UNDEFINED;
            } else if (str.equals(BOOL_TRUE_STR)) {
                return lastToken = Token.TRUE;
            } else if (str.equals(BOOL_FALSE_STR)) {
                return lastToken = Token.FALSE;
            }
            return lastToken = Token.KEY;
        } else if (isFloatingNumber(ch)) {
            int lastIndex = getPastLastIndex(input, index, new MatchRange() {
                @Override
                public boolean matches(char ch) {
                    return isFloatingNumber(ch);
                }
            });
            str = input.substring(index - 1, lastIndex);
            number = Double.valueOf(str);
            index = lastIndex;
            return lastToken = Token.NUMBER;
        }

        return lastToken = Token.NONE;
    }

    public String getStr() {
        return str;
    }

    public Double getNumber() {
        return number;
    }

    public Token getLastToken() {
        return lastToken;
    }

    public int getPrevIndex() {
        return prevIndex;
    }

    public interface MatchRange {
        boolean matches(char ch);
    }
}
