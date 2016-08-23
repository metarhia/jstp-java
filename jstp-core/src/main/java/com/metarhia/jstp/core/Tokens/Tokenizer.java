package com.metarhia.jstp.core.Tokens;

import com.metarhia.jstp.core.JSParsingException;
import com.metarhia.jstp.core.JSTypes.JSBool;
import com.metarhia.jstp.core.JSTypes.JSNull;
import com.metarhia.jstp.core.JSTypes.JSUndefined;

/**
 * Created by lundibundi on 7/4/16.
 */
public class Tokenizer {
    private static CharRange numberRange = new CharRange(0x30, 0x39);
    private static CharRange letterUpperRange = new CharRange(0x41, 0x5a);
    private static CharRange letterLowerRange = new CharRange(0x61, 0x7a);

    private Double number;
    private String str;

    private String input;
    private int index;

    private Token lastToken;

    public Tokenizer(String input) {
        number = null;
        str = null;
        this.input = input;
        index = 0;
        lastToken = null;
    }

    public Token next() throws JSParsingException {
        // reset variables
        str = null;
        number = null;

        char ch = 0;

        if (index >= input.length()) return lastToken = Token.NONE;

        do {
            ch = input.charAt(index);
        } while (++index < input.length()
                && (ch == 0x20 || ch == 0x0a || ch == 0x09)); // space and \n and \t

//        if (index >= input.length()) return lastToken = Token.NONE;

        switch (ch) {
            case '[':
            case ']':
            case '{':
            case '}':
            case ':':
            case ',':
                return lastToken = Token.fromString(ch);
        }

        if (ch == 0x22 || ch == 0x27) { // double and single quotes
//        if (ch == '"' || ch == '\'') {
            if (index >= input.length()) {
                throw new JSParsingException("Error: no closing quote '" + ch + "'");
            }
            int lastIndex = input.indexOf(ch, index);
            str = input.substring(index, lastIndex);
            index = lastIndex + 1; // skip quote
            return lastToken = Token.STRING;
        }

//        if (ch == '_' || Character.isLetter(ch)) {
        if (ch == 0x5f || isLetter(ch)) { // underscore
            // identifier
            int lastIndex = getPastLastIndex(input, index);
            str = input.substring(index - 1, lastIndex);
            index = lastIndex;

            if (str.equals(JSNull.get().toString())) {
                return lastToken = Token.NULL;
            } else if (str.equals(JSUndefined.get().toString())) {
                return lastToken = Token.UNDEFINED;
            } else if (str.equals(new JSBool(true).toString())) {
                return lastToken = Token.TRUE;
            } else if (str.equals(new JSBool(false).toString())) {
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

        // TODO what is NONE?
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

    public interface MatchRange {
        boolean matches(char ch);
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
}
