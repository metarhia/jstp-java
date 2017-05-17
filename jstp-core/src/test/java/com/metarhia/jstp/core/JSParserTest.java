package com.metarhia.jstp.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.metarhia.jstp.core.JSTypes.JSArray;
import com.metarhia.jstp.core.JSTypes.JSBool;
import com.metarhia.jstp.core.JSTypes.JSNull;
import com.metarhia.jstp.core.JSTypes.JSNumber;
import com.metarhia.jstp.core.JSTypes.JSObject;
import com.metarhia.jstp.core.JSTypes.JSString;
import com.metarhia.jstp.core.JSTypes.JSUndefined;
import com.metarhia.jstp.core.JSTypes.JSValue;
import com.metarhia.jstp.core.TestUtils.TestData;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class JSParserTest {

  private static final TestData[] parseTestData = new TestData[]{
      new TestData<>("[ 'abs', 'smth else', \" or like this \", ['inside', 'elsein']]",
          new JSArray("abs", "smth else", " or like this ",
              new JSArray("inside", "elsein"))),
      new TestData<>("[1,,300]", new JSArray(1.0, JSUndefined.get(), 300.0)),
      new TestData<>("{a:1,b: 2.0,c:'5555'}", new JSObject(
          TestUtils.mapOf(
              "a", new JSNumber(1),
              "b", new JSNumber(2.0),
              "c", new JSString("5555")
          )
      )),
      new TestData<>("[,,0]", new JSArray(
          JSUndefined.get(), JSUndefined.get(), new JSNumber(0))),
      new TestData<>("{nickname:'\\n\\tnyaaaaaa\\'aaa\\'[((:’ –( :-)) :-| :~ =:O)],'}",
          new JSObject(TestUtils.mapOf(
              "nickname",
              new JSString("\n\tnyaaaaaa\'aaa\'[((:’ –( :-)) :-| :~ =:O)],")))),
      new TestData<>("{nickname:\"\\n\\tnyaaaaaa'aaa'[((:’ –( :-)) :-| :~ =:O)],\"}",
          new JSObject(TestUtils.mapOf(
              "nickname",
              new JSString("\n\tnyaaaaaa\'aaa\'[((:’ –( :-)) :-| :~ =:O)],")))),

      new TestData<>("[1,2,'5555']", new JSArray(1, 2, "5555")),
      new TestData<>("true", new JSBool(true)),
      new TestData<>("false", new JSBool(false)),
      new TestData<>("10", new JSNumber(10)),
      new TestData<>("63.52", new JSNumber(63.52)),
      new TestData<>("undefined", JSUndefined.get()),
      new TestData<>("null", JSNull.get()),
      new TestData<>("{birth:-2051225940000}", new JSObject(
          TestUtils.mapOf("birth", new JSNumber(-2051225940000L))))
  };

  private static final ArrayList<TestData<String, String>> stringifyTestData =
      new ArrayList<>(Arrays.asList(
          new TestData<>("{}", "{}"),
          new TestData<>("'abv\\\"gggg\\\"dd'", "'abv\"gggg\"dd'"),
          new TestData<>("'abv\"gggg\"dd'", "'abv\"gggg\"dd'"),
          new TestData<>("['outer', ['inner']]", "[\'outer\',[\'inner\']]"),
          new TestData<>("\'\\u{1F49A}ttt\\u{1F49B}\'", "'💚ttt💛'"),
          new TestData<>("'\\x20'", "' '")));

  static {
    for (int i = 7; i < parseTestData.length; i++) {
      String input = (String) parseTestData[i].input;
      stringifyTestData.add(new TestData<>(input, input));
    }
  }

  private static final TestData[] parseKeyValuePairTestData = new TestData[]{
      new TestData<>("a: 4", new JSObject.Entry("a", 4)),
      new TestData<>("55 : ['abc']", new JSObject.Entry("55.0", new JSArray("abc")))
  };

  private static final TestData[] parseThrowTestData = new TestData[]{
      new TestData<>("", new JSParsingException(
          "Index: 0, Message: Cannot parse")),
      new TestData<>("{he : llo : 123}", new JSParsingException(
          "Index: 6, Message: llo is not defined")),
      new TestData<>("{he : 'llo'  : 123}", new JSParsingException(
          "Index: 13, Message: Expected ',' as key-value pairs separator")),
      new TestData<>("{'ssssss : }", new JSParsingException(
          "Index: 1, Message: Unmatched quote")),
      new TestData<>("'ssssss", new JSParsingException(
          "Index: 0, Message: Unmatched quote")),
      new TestData<>("{a:", new JSParsingException(
          "Index: 2, Message: Expected value after ':' in object")),
      new TestData<>("{a:}", new JSParsingException(
          "Index: 3, Message: Expected value after ':' in object"))
  };

  @Test
  public void parseTest() throws Exception {
    JSParser parser = new JSParser();
    for (TestData<String, Object> td : parseTestData) {
      parser.setInput(td.input);
      JSValue actual = parser.parse();
      assertEquals(td.expected, actual, "Failed parsing: " + td.input);
    }
  }

  @Test
  public void parseKeyValuePair() throws Exception {
    JSParser parser = new JSParser();
    for (TestData<String, JSObject.Entry> td : parseKeyValuePairTestData) {
      parser.setInput(td.input);
      JSObject.Entry actual = parser.parseKeyValuePair();
      assertEquals(td.expected.key, actual.key, "Failed parsing(key): " + td.input);
      assertEquals(td.expected.value, actual.value, "Failed parsing(value): " + td.input);
    }
  }

  @Test
  public void parseThrow() throws Exception {
    JSParser parser = new JSParser();
    for (TestData<String, JSParsingException> td : parseThrowTestData) {
      Exception exception = null;
      try {
        parser.setInput(td.input);
        parser.parse();
      } catch (JSParsingException e) {
        exception = e;
      }
      assertNotNull(exception);
      assertEquals(td.expected.getMessage(), exception.getMessage(),
          "Failed parsing(throw): " + td.input);
    }
  }

  @Test
  public void stringifyTestData() throws Exception {
    for (TestData<String, String> td : stringifyTestData) {
      JSValue actual = new JSParser(td.input).parse();
      assertEquals(td.expected, actual.toString());
    }
  }

  @Test
  public void setInput() throws Exception {
    String input = "true";
    String anotherInput = "false";

    JSParser parser = new JSParser(input);

    parser.setInput(anotherInput);

    assertEquals(parser.parse(), new JSBool(false));
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
