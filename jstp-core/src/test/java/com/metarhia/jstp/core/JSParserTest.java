package com.metarhia.jstp.core;

import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSParsingException;
import com.metarhia.jstp.core.JSTypes.*;

import org.junit.Test;
import org.junit.internal.JUnitSystem;

import java.util.Arrays;
import java.util.Collections;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by lundibundi on 7/6/16.
 */
public class JSParserTest {
    @Test
    public void parseArray() throws Exception {
        String input = "[1, 2, '5555']";
        JSArray expected = new JSArray(Arrays.<Object>asList(1, 2, "5555"));

        JSParser parser = new JSParser(input);
        JSArray actual = parser.parseArray();

        assertEquals(expected, actual);
    }

    @Test
    public void parseObject() throws Exception {
        String input = "{a: 1, b: 2.0, c: '5555'}";
        JSObject expected = new JSObject();
        expected.put("a", new JSNumber(1));
        expected.put("b", new JSNumber(2.0));
        expected.put("c", new JSString("5555"));

        JSParser parser = new JSParser(input);
        JSObject actual = parser.parseObject();

        assertEquals(expected, actual);
    }

    @Test
    public void parseKeyValuePair() throws Exception {
        String[] inputs = {"a: 4", "55 : ['abc']"};

        JSObject.Entry[] expecteds = {
                new JSObject.Entry("a", 4),
                new JSObject.Entry("55",
                        new JSArray(Collections.<Object>singletonList("abc")))
        };

        JSParser parser = new JSParser();
        for (int i = 0; i < inputs.length; i++) {
            parser.setInput(inputs[i]);

            final JSObject.Entry expected = expecteds[i];
            final JSObject.Entry actual = parser.parseKeyValuePair();

            assertEquals(expected.toString(), actual.toString());
        }
    }

    @Test
    public void setInput() throws Exception {
        String input = "true";
        String anotherInput = "false";

        JSParser parser = new JSParser(input);

        parser.setInput(anotherInput);

        assertEquals(parser.parseDeduce(), new JSBool(false));
    }

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
    public void testPacketSample() throws Exception {
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
                "      room: ['158', '111', '555']\n" +
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
        JSArray roomArray = new JSArray();
        roomArray.add(new JSString("158"));
        roomArray.add(new JSString("111"));
        roomArray.add(new JSString("555"));
        nnAddress.put("room", roomArray);
        nestedContacts.put("address", nnAddress);
        expected.put("contacts", nestedContacts);


        JSParser parser = new JSParser(input);
        JSObject actual = parser.parseObject();

        assertEquals(expected, actual);
    }
}
