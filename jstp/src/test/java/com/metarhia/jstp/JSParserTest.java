package com.metarhia.jstp;

import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSParsingException;
import com.metarhia.jstp.core.JSTypes.JSArray;
import com.metarhia.jstp.core.JSTypes.JSBool;
import com.metarhia.jstp.core.JSTypes.JSNull;
import com.metarhia.jstp.core.JSTypes.JSNumber;
import com.metarhia.jstp.core.JSTypes.JSObject;
import com.metarhia.jstp.core.JSTypes.JSString;
import com.metarhia.jstp.core.JSTypes.JSUndefined;
import com.metarhia.jstp.core.JSTypes.JSValue;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by lundibundi on 7/6/16.
 */
public class JSParserTest {
    @Test
    public void parseDeduce() throws Exception {
        String input = "true false 10 63.52 undefined null";
        JSParser parser = new JSParser(input);
        try {
            JSValue value = parser.parseDeduce();
            assertEquals("Parsing 'true' failed", new JSBool(true), value);

            value = parser.parseDeduce();
            assertEquals("Parsing 'false' failed", new JSBool(false), value);

            value = parser.parseDeduce();
            assertEquals("Parsing decimal failed", new JSNumber(10), value);

            value = parser.parseDeduce();
            assertEquals("Parsing double failed", new JSNumber(63.52), value);

            value = parser.parseDeduce();
            assertEquals("Parsing 'undefined' failed", JSUndefined.get(), value);

            value = parser.parseDeduce();
            assertEquals("Parsing 'null' failed", JSNull.get(), value);

            //test array
            input = "[ 'abs', 'smth else', \" or like this \", ['inside', 'elsein']]";
            JSArray nestedArray = new JSArray();
            nestedArray.add(new JSString("inside"));
            nestedArray.add(new JSString("elsein"));

            JSArray expected = new JSArray();
            expected.add(new JSString("abs"));
            expected.add(new JSString("smth else"));
            expected.add(new JSString(" or like this "));
            expected.add(nestedArray);

            parser.setInput(input);
            value = parser.parseDeduce();
            assertTrue("Parsing array failed", value instanceof JSArray);
            assertEquals("Parsing array failed", expected, value);
        } catch (JSParsingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void parseDeduce1() throws Exception {

    }

    @Test
    public void parseArray() throws Exception {

    }

    @Test
    public void parseObject() throws Exception {
        String input = "{\n" +
                "  name: 'Marcus Aurelius',\n" +
                "  passport: 'AE127095',\n" +
                "  birth: {\n" +
                "    date: '1990-02-15',\n" +
                "    place: 'Rome'\n" +
                "  },\n" +
                "  contacts: {\n" +
                "    email: 'marcus@aurelius.it',\n" +
                "    phone: '+380505551234',\n" +
                "    address: {\n" +
                "      country: 'Ukraine',\n" +
                "      city: 'Kiev',\n" +
                "      zip: '03056',\n" +
                "      street: 'Pobedy',\n" +
                "      building: '37',\n" +
                "      floor: '1',\n" +
                "      room: '158'\n" +
                "    }\n" +
                "  }\n" +
                "}";

        JSObject expected = new JSObject();
        expected.put("name", new JSString("Marcus Aurelius"));
        expected.put("passport", new JSString("AE127095"));
        JSObject nestedBirth = new JSObject();
        nestedBirth.put("date", new JSString("1990-02-15"));
        nestedBirth.put("place", new JSString("Rome"));
        expected.put("birth", nestedBirth);
        JSObject nestedContacts = new JSObject();
        nestedContacts.put("email", new JSString("marcus@aurelius.it"));
        nestedContacts.put("phone", new JSString("+380505551234"));
        JSObject nnAddress = new JSObject();
        nnAddress.put("country", new JSString("Ukraine"));
        nnAddress.put("city", new JSString("Kiev"));
        nnAddress.put("zip", new JSString("03056"));
        nnAddress.put("street", new JSString("Pobedy"));
        nnAddress.put("building", new JSString("37"));
        nnAddress.put("floor", new JSString("1"));
        nnAddress.put("room", new JSString("158"));
        nestedContacts.put("address", nnAddress);
        expected.put("contacts", nestedContacts);


        JSObject actual = JSTP.parse(input);

        assertEquals(expected, actual);
    }

    @Test
    public void parseKeyValuePair() throws Exception {

    }
}
