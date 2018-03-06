package com.metarhia.jstp;

import com.metarhia.jstp.connection.AppData;
import com.metarhia.jstp.connection.Connection;
import com.metarhia.jstp.connection.SimpleConnectionListener;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSParsingException;
import com.metarhia.jstp.core.JSSerializer;
import com.metarhia.jstp.transport.TCPTransport;
import java.io.Serializable;

/**
 * Common JSTP interface
 */
public final class JSTP implements Serializable {

  private JSTP() {
  }

  /**
   * Wrapper method for {@link JSParser#parse(String)}
   */
  public static <T> T parse(String input) throws JSParsingException {
    return JSParser.parse(input);
  }

  /**
   * Wrapper method for {@link JSSerializer#stringify(Object)}
   */
  public static <T> String stringify(T value) {
    return JSSerializer.stringify(value);
  }

  public static void connectTCP(String app, String host, int port,
                                final ConnectCallback callback) {
    TCPTransport transport = new TCPTransport(host, port);
    Connection connection = new Connection(transport);
    connection.setAppData(AppData.valueOf(app));
    connectTCP(connection, null, callback);
  }

  public static void connectTCP(final Connection connection,
                                String host, int port,
                                final ConnectCallback callback) {
    connectTCP(connection, new TCPTransport(host, port), callback);
  }

  public static void connectTCP(final Connection connection,
                                final TCPTransport transport,
                                final ConnectCallback callback) {
    if (connection.getAppName() == null) {
      throw new IllegalArgumentException("Connection must have application data");
    }
    connection.addListener(new SimpleConnectionListener() {
      @Override
      public void onConnected(boolean restored) {
        connection.removeListener(this);
        callback.onConnected(connection);
      }

      @Override
      public void onConnectionClosed() {
        connection.removeListener(this);
        callback.onError(null);
      }
    });
    if (transport != null) {
      connection.useTransport(transport);
    } else {
      connection.connect(connection.getAppData());
    }
  }

  public interface ConnectCallback {

    void onConnected(Connection connection);

    void onError(Exception e);
  }
}
