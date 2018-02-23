package com.metarhia.jstp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.metarhia.jstp.connection.AbstractSocket;
import com.metarhia.jstp.connection.Connection;

public class TestUtils {

  public static ConnectionSpy createConnectionSpy() {
    return createConnectionSpy(null, true);
  }

  public static ConnectionSpy createConnectionSpy(AbstractSocket transport,
                                                  boolean transportConnected) {
    if (transport == null) {
      transport = mock(AbstractSocket.class);
      when(transport.isConnected()).thenReturn(transportConnected);
    }
    Connection connection = spy(new Connection(transport));
    connection.getSessionPolicy().setConnection(connection);
    return new ConnectionSpy(connection, transport);
  }

  public static class ConnectionSpy {

    public Connection connection;

    public AbstractSocket transport;

    public ConnectionSpy(Connection connection, AbstractSocket transport) {
      this.connection = connection;
      this.transport = transport;
    }
  }

  public static class TestData<T, F> {

    public T input;
    public F expected;

    public TestData(T input, F expected) {
      this.input = input;
      this.expected = expected;
    }
  }
}
