package com.metarhia.jstp.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.metarhia.jstp.TestUtils.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class AppDataTest {

  private static final TestData<String, AppData>[] validTestData = new TestData[]{
      new TestData<>("name", new AppData("name", null)),
      new TestData<>("name@1.0.0", new AppData("name", "1.0.0"))
  };

  private static final TestData<String, Class>[] invalidTestData = new TestData[]{
      new TestData<>("", IllegalArgumentException.class),
      new TestData<>("@1.0.0", IllegalArgumentException.class),
      new TestData<>("name@+13", RuntimeException.class)
  };

  @Test
  void valueOf() {
    for (TestData<String, AppData> td : validTestData) {
      AppData actual = AppData.valueOf(td.input);
      assertEquals(td.expected, actual, "Failed parsing: " + td.input);
    }

    for (final TestData<String, Class> td : invalidTestData) {
      assertThrows(td.expected, new Executable() {
        @Override
        public void execute() throws Throwable {
          AppData.valueOf(td.input);
        }
      }, "Didn't throw for: " + td.input);
    }
  }

  @Test
  void getApp() {
    AppData appData1 = new AppData("name");
    assertEquals("name", appData1.getApp(),
        "Must correctly serialize AppData without version");

    AppData appData2 = new AppData("name", "1.0.0");
    assertEquals("name@1.0.0", appData2.getApp(),
        "Must correctly serialize full AppData");
  }
}
