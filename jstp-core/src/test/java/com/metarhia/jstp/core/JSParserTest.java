package com.metarhia.jstp.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSTypes.JSEntry;
import com.metarhia.jstp.core.JSTypes.JSUndefined;
import com.metarhia.jstp.core.TestUtils.TestData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Created by lundibundi on 4/1/17.
 */
class JSParserTest {

  public static final TestData[] parseTestData = new TestData[]{
      new TestData<>("[,,0]", Arrays.asList(
          JSUndefined.get(), JSUndefined.get(), 0)),
      new TestData<>("{nickname: '\\n\\tnyaaaaaa\\'aaa\\'[((:’ –( :-)) :-| :~ =:O)],'}",
          TestUtils.mapOf(
              "nickname", "\n\tnyaaaaaa\'aaa\'[((:’ –( :-)) :-| :~ =:O)],")),
      new TestData<>("{nickname:\"\\n\\tnyaaaaaa'aaa'[((:’ –( :-)) :-| :~ =:O)],\"}",
          TestUtils.mapOf(
              "nickname", "\n\tnyaaaaaa\'aaa\'[((:’ –( :-)) :-| :~ =:O)],")),
      new TestData<>("[ 'abs', 'smth else', \" or like this \", ['inside', 'elsein']]",
          Arrays.asList("abs", "smth else", " or like this ",
              Arrays.asList("inside", "elsein"))),
      new TestData<>("{a: 1, b: 2.0, c: '5555'}", TestUtils.mapOf(
          "a", 1,
          "b", 2.0,
          "c", "5555")
      ),
      new TestData<>("[1,,300]", Arrays.asList(1, JSUndefined.get(), 300)),
      new TestData<>("10.", 10.0),
      new TestData<>(".10", .1),
      new TestData<>("+Infinity", Double.POSITIVE_INFINITY),

      new TestData<>("[1,2,'5555']", Arrays.asList(1, 2, "5555")),
      new TestData<>("true", true),
      new TestData<>("false", false),
      new TestData<>("10", 10),
      new TestData<>("63.52", 63.52),
      new TestData<>("23051225940000", 23051225940000L),
      new TestData<>("NaN", Double.NaN),
      new TestData<>("Infinity", Double.POSITIVE_INFINITY),
      new TestData<>("-Infinity", Double.NEGATIVE_INFINITY),
      new TestData<>("undefined", JSUndefined.get()),
      new TestData<>("null", null),
      new TestData<>("{birth:-2051225940000}", TestUtils.mapOf(
          "birth", -2051225940000L)),
  };

  private static final TestData[] parseKeyValuePairTestData = new TestData[]{
      new TestData<>("\\u{0061}bc: 4", new JSEntry<>("abc", 4)),
      new TestData<>("\\u0061bc: 4", new JSEntry<>("abc", 4)),
      new TestData<>("a: 4", new JSEntry<>("a", 4)),
      new TestData<>("_a: 4", new JSEntry<>("_a", 4)),
      new TestData<>("$a: 4", new JSEntry<>("$a", 4)),
      new TestData<>("55 : ['abc']", new JSEntry<>("55", Arrays.asList("abc")))
  };

  private static final TestData[] parseThrowTestData = new TestData[]{
      new TestData<>("{he : llo : 123}",
          new JSParsingException(6, "llo is not defined")),
      new TestData<>("{he : 'llo'  : 123}",
          new JSParsingException(13, "Expected ',' as key-value pairs separator")),
      new TestData<>("{'ssssss : }",
          new JSParsingException(1, "Unmatched quote")),
      new TestData<>("'ssssss",
          new JSParsingException(0, "Unmatched quote")),
      new TestData<>("{a:",
          new JSParsingException(2, "Expected value after ':' in object")),
      new TestData<>("{a:}",
          new JSParsingException(3, "Expected value after ':' in object")),
      new TestData<>("{:2}",
          new JSParsingException(1, "Expected valid key")),
  };

  private JSParser parser;

  public JSParserTest() {
    parser = new JSParser();
  }

  @Test
  public void parseTest() throws Exception {
    for (TestData<String, Object> td : parseTestData) {
      try {
        parser.setInput(td.input);
        Object actual = parser.parse();
        assertEquals(td.expected, actual, "Failed parsing: " + td.input);
      } catch (JSParsingException e) {
        fail("Cannot parse " + td.input, e);
      }
    }
  }

  @Test
  public void parseKeyValuePair() throws Exception {
    for (TestData<String, JSEntry> td : parseKeyValuePairTestData) {
      try {
        parser.setInput(td.input);
        JSEntry actual = parser.parseKeyValuePair();
        assertEquals(td.expected.getKey(), actual.getKey(),
            "Failed parsing(key): " + td.input);
        assertEquals(td.expected.getValue(), actual.getValue(),
            "Failed parsing(value): " + td.input);
      } catch (JSParsingException e) {
        fail("Cannot parse " + td.input, e);
      }
    }
  }

  @Test
  public void parseThrow() throws Exception {
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
  public void testSampleMessage() throws Exception {
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

    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("name", "Marcus Aurelius");
    expected.put("passport", "AE127095");

    Map<String, Object> nestedBirth = new LinkedHashMap<>();
    nestedBirth.put("date", "1990-02-15");
    nestedBirth.put("place", "Rome");
    expected.put("birth", nestedBirth);
    Map<String, Object> nestedContacts = new LinkedHashMap<>();
    nestedContacts.put("email", "marcus@aurelius.it");
    nestedContacts.put("phone", "+380505551234");
    Map<String, Object> nnAddress = new LinkedHashMap<>();
    nnAddress.put("country", "Ukraine");
    nnAddress.put("city", "Kiev");
    nnAddress.put("zip", "03056");
    nnAddress.put("street", "Pobedy");
    nnAddress.put("building", "37");
    nnAddress.put("floor", "1");
    List<Object> roomArray = new ArrayList<>();
    roomArray.add("158");
    roomArray.add("111");
    roomArray.add("555");
    nnAddress.put("room", roomArray);
    nestedContacts.put("address", nnAddress);
    expected.put("contacts", nestedContacts);

    parser.setInput(input);
    JSObject<?> actual = parser.parseObject();

    assertEquals(expected, actual);
  }
}
