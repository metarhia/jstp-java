package com.metarhia.jstp.transport;

import com.metarhia.jstp.Constants;
import com.metarhia.jstp.exceptions.AlreadyConnectedException;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP transport for JSTP connection
 */
public class TCPTransport implements Transport {

  /**
   * Default closing tick
   */
  public static final long DEFAULT_CLOSING_TICK = 1000;

  /**
   * Default closing timeout
   */
  public static final long DEFAULT_CLOSING_TIMEOUT = 5000;

  /**
   * Default message size
   */
  public static final int DEFAULT_MESSAGE_SIZE = 100;

  private static final Logger logger = LoggerFactory.getLogger(TCPTransport.class);

  private final Object senderLock = new Object();
  private final Object pauseLock = new Object();

  private String host;
  private int port;
  private boolean sslEnabled;
  private boolean running;
  private boolean connecting;
  private boolean closing;
  private Thread receiverThread;
  private Thread senderThread;
  private Queue<String> messageQueue;
  private Socket socket;

  private OutputStream out;
  private BufferedInputStream in;

  private long closingTick;
  private long closingTimeout;

  private TransportListener socketListener;

  /**
   * Creates new TCP transport instance with specified host and port (SSL is disabled by default)
   *
   * @param host server host
   * @param port server port
   */
  public TCPTransport(String host, int port) {
    this(host, port, null);
  }

  /**
   * Creates new TCP transport instance with specified host, port and socket listener (SSL is
   * disabled by default)
   *
   * @param host     server host
   * @param port     server port
   * @param listener socket events listener
   */
  public TCPTransport(String host, int port, TransportListener listener) {
    this(host, port, true, listener);
  }

  /**
   * Creates new TCP transport instance with specified host, port and SSL enabled or disabled
   *
   * @param host       server host
   * @param port       server port
   * @param sslEnabled is SSL enabled or disabled
   */
  public TCPTransport(String host, int port, boolean sslEnabled) {
    this(host, port, sslEnabled, null);
  }

  /**
   * Creates new TCP transport instance with specified host, port, socket listener and SSL enabled
   * or disabled
   *
   * @param host       server host
   * @param port       server port
   * @param sslEnabled is SSL enabled or disabled
   * @param listener   socket events listener
   */
  public TCPTransport(String host, int port, boolean sslEnabled,
                      TransportListener listener) {
    this.closingTick = DEFAULT_CLOSING_TICK;
    this.closingTimeout = DEFAULT_CLOSING_TIMEOUT;

    this.host = host;
    this.port = port;
    this.sslEnabled = sslEnabled;
    this.socketListener = listener;
    this.messageQueue = new ConcurrentLinkedQueue<>();
  }

  @Override
  public boolean connect() {
    return connect(null);
  }

  public boolean connect(final ConnectCallback callback) {
    return connect(this.host, this.port, callback);
  }

  public synchronized boolean connect(String host, int port, final ConnectCallback callback) {
    if (isConnected()) {
      AlreadyConnectedException error = new AlreadyConnectedException();
      if (socketListener != null) {
        socketListener.onTransportError(error);
      }
      errorCallback(callback, error);
      return false;
    }

    if (connecting) {
      return false;
    }

    this.host = host;
    this.port = port;

    connecting = true;
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          boolean connected;
          synchronized (TCPTransport.this) {
            if (closing) {
              closeInternal(false);
            }
            connected = initConnection();
            if (connected) {
              startReceiverThread();
              startSenderThread();
            }
            connecting = false;
          }
          if (connected) {
            if (socketListener != null) {
              socketListener.onTransportConnected();
            }
            connectCallback(callback);
          } else {
            closeInternal();
            errorCallback(callback, null);
          }
        } catch (Exception e) {
          connecting = false;
          logger.info("Cannot create socket: ", e);
          errorCallback(callback, e);
          closeInternal();
        }
      }
    }).start();
    return true;
  }

  private void connectCallback(ConnectCallback callback) {
    if (callback != null) {
      callback.onConnected(TCPTransport.this);
    }
  }

  private void errorCallback(ConnectCallback callback, Exception e) {
    if (callback != null) {
      callback.onError(e);
    }
  }

  private void startSenderThread() {
    if (this.senderThread != null) {
      throw new RuntimeException("Starting new sender thread before closing the previous one");
    }

    this.senderThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          String message;
          while (!closing) {
            while (running) {
              while (messageQueue.isEmpty()) {
                synchronized (senderLock) {
                  if (messageQueue.isEmpty()) {
                    senderLock.wait();
                  }
                }
              }
              message = messageQueue.poll();
              if (message != null) {
                sendMessageInternal(message);
              }
            }
            synchronized (pauseLock) {
              if (!running) {
                pauseLock.wait();
              }
            }
          }
        } catch (InterruptedException | ClosedByInterruptException e) {
          // all ok - manually closing
        } catch (Exception e) {
          logger.info("Sender thread failed", e);
          closeInternal();
        }
      }
    });
    this.senderThread.start();
  }

  private void sendMessageInternal(String message) throws IOException {
    logger.trace("Sending message: {}", message);

    out.write(message.getBytes(Constants.UTF_8_CHARSET));
    out.write(Constants.SEPARATOR);
    out.flush();
  }

  private synchronized void startReceiverThread() {
    if (this.receiverThread != null) {
      throw new RuntimeException("Starting new receiver thread before closing the previous one");
    }

    final ByteArrayOutputStream localMessageBuilder =
        new ByteArrayOutputStream(DEFAULT_MESSAGE_SIZE);
    final BufferedInputStream localIn = in;
    this.receiverThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          while (!closing) {
            while (running) {
              processMessage(localIn, localMessageBuilder);
            }
            synchronized (pauseLock) {
              if (!running) {
                pauseLock.wait();
              }
            }
          }
        } catch (InterruptedException | ClosedByInterruptException e) {
          // all ok - manually closing
        } catch (Exception e) {
          logger.info("Receiver thread failed", e);
          closeInternal();
        }
      }
    });
    this.receiverThread.start();
  }

  void processMessage(BufferedInputStream in, ByteArrayOutputStream messageBuilder)
      throws IOException {
    int b = 0;
    while ((b = in.read()) > 0) {
      messageBuilder.write(b);
    }
    if (messageBuilder.size() != 0 && socketListener != null) {
      messageBuilder.write('\0');
      String message = messageBuilder.toString(Constants.UTF_8_CHARSET_NAME);

      logger.trace("Received message: {}", message);

      socketListener.onMessageReceived(message);
    }
    messageBuilder.reset();
    if (b == -1) {
      logger.trace("Remote host closed connection (Input steam closed)");
      closeInternal();
    }
  }

  private boolean initConnection() throws IOException {
    if (socket == null || !socket.isConnected() || socket.isClosed()) {
      if (sslEnabled) {
        socket = createSSLSocket(host, port);
      } else {
        socket = new Socket(host, port);
      }
    }

    if (socket != null && socket.isConnected()) {
      logger.trace("Created socket: {}:{}", host, port);
      running = true;
      out = socket.getOutputStream();
      in = new BufferedInputStream(socket.getInputStream());
      return true;
    }
    return false;
  }

  private Socket createSSLSocket(String host, int port) throws IOException {
    try {
      SSLContext context = SSLContext.getInstance("TLSv1.2");
      context.init(null, null, null);
      Socket socket = context.getSocketFactory().createSocket(host, port);
      verifySSLHostname((SSLSocket) socket);
      return socket;
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      logger.warn("Cannot create SSL socket", e);
    }
    return null;
  }

  private void verifySSLHostname(SSLSocket socket) throws IOException {
    HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
    SSLSession session = socket.getSession();
    if (!hv.verify(host, socket.getSession())) {
      throw new SSLHandshakeException(
          "Expected " + host + ", found " + session.getPeerPrincipal());
    }
  }

  @Override
  public void send(String message) {
    messageQueue.add(message);
    if (running) {
      synchronized (senderLock) {
        senderLock.notify();
      }
    }
  }

  @Override
  public void clearQueue() {
    messageQueue.clear();
  }

  public void pause() {
    this.running = false;
  }

  public void resume() {
    running = true;
    synchronized (pauseLock) {
      pauseLock.notifyAll();
    }
    if (!messageQueue.isEmpty()) {
      synchronized (senderLock) {
        senderLock.notify();
      }
    }
  }

  @Override
  public void close(final boolean forced) {
    synchronized (TCPTransport.this) {
      if (closing) {
        return;
      }
      logger.trace("Public close transport");
      closing = true;
    }

    if (forced || messageQueue.isEmpty()) {
      closeInternal();
      return;
    }

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          closing = true;
          synchronized (this) {
            long delay = 0;
            while (delay <= DEFAULT_CLOSING_TIMEOUT && !messageQueue.isEmpty()) {
              wait(DEFAULT_CLOSING_TICK);
              delay += DEFAULT_CLOSING_TICK;
            }
          }
          synchronized (TCPTransport.this) {
            if (closing) {
              closeInternal();
            }
          }
        } catch (InterruptedException e) {
          // all ok - manually closing
        }
        closing = false;
      }
    }).start();
  }

  private void closeInternal() {
    closeInternal(true);
  }

  private void closeInternal(boolean notify) {
    logger.trace("Internal close transport");
    try {
      synchronized (TCPTransport.this) {
        closing = true;
        clearQueue();
        running = false;
        if (receiverThread != null) {
          receiverThread.interrupt();
        }
        if (senderThread != null) {
          senderThread.interrupt();
        }
        receiverThread = null;
        senderThread = null;
        if (in != null) {
          in.close();
        }
        if (socket != null) {
          socket.close();
        }
        in = null;
        socket = null;
        closing = false;
      }
      if (notify && socketListener != null) {
        socketListener.onTransportClosed();
      }
    } catch (IOException e) {
      logger.info("Socket closing failure", e);
    }
  }

  @Override
  public boolean isConnected() {
    return !connecting && socket != null && socket.isConnected();
  }

  @Override
  public boolean isClosed() {
    return socket == null || socket.isClosed();
  }

  public String getHost() {
    return host;
  }

  /**
   * Sets server host to specified host
   *
   * @param host specified host
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Gets server port
   *
   * @return server port
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets server port to specified port
   *
   * @param port specified port
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Checks if SSL is enabled
   *
   * @return true is SSL is enabled and false otherwise
   */
  public boolean isSSLEnabled() {
    return sslEnabled;
  }

  /**
   * Sets SSL enabled or disabled
   *
   * @param sslEnabled true is SSL enabled and false otherwise
   */
  public void setSSLEnabled(boolean sslEnabled) {
    this.sslEnabled = sslEnabled;
  }

  /**
   * Gets closing tick
   *
   * @return closing tick
   */
  public long getClosingTick() {
    return closingTick;
  }

  /**
   * Sets closing tick to specified
   *
   * @param closingTick specified closing tick
   */
  public void setClosingTick(long closingTick) {
    this.closingTick = closingTick;
  }

  /**
   * Gets closing timeout
   *
   * @return closing timeout
   */
  public long getClosingTimeout() {
    return closingTimeout;
  }

  @Override
  public void setListener(TransportListener listener) {
    socketListener = listener;
  }

  /**
   * Sets closing timeout to specified
   *
   * @param closingTimeout specified closing timeout
   */
  public void setClosingTimeout(long closingTimeout) {
    this.closingTimeout = closingTimeout;
  }

  public interface ConnectCallback {

    void onConnected(TCPTransport transport);

    void onError(Exception e);
  }
}
