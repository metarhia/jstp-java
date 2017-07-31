package com.metarhia.jstp.transport;

import com.metarhia.jstp.Constants;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSParsingException;
import com.metarhia.jstp.core.JSSerializer;
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
import javax.net.ssl.SSLContext;
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

  private JSParser jsParser;

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
    this(host, port, false, listener);
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
    messageQueue = new ConcurrentLinkedQueue<>();
    jsParser = new JSParser();
  }

  @Override
  public boolean connect() {
    if (isConnected()) {
      if (socketListener != null) {
        socketListener.onTransportError(new AlreadyConnectedException());
      }
      return false;
    }

    connecting = true;
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          if (closing) {
            closeInternal(false);
          }
          if (initConnection()) {
            startReceiverThread();
            startSenderThread();
          } else {
            closeInternal();
          }
        } catch (IOException e) {
          logger.info("Cannot create socket", e);
          closeInternal();
        }
      }
    }).start();
    return true;
  }

  private synchronized void startSenderThread() {
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
        } catch (IOException e) {
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
        } catch (InterruptedException | ClosedByInterruptException | NullPointerException e) {
          // all ok - manually closing
          // npe - 'in' was null, means we are closing transport right now
        } catch (IOException e) {
          if (!Thread.currentThread().isInterrupted()) {
            // means this thread was closed
            logger.info("Receiver thread failed", e);
            closeInternal();
          }
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
    if (Thread.currentThread().isInterrupted()) {
      return;
    }
    if (messageBuilder.size() != 0 && socketListener != null) {
      messageBuilder.write('\0');
      String message = messageBuilder.toString(Constants.UTF_8_CHARSET_NAME);

      logger.trace("Received message: {}", message);

      jsParser.setInput(message);
      try {
        final Object parseResult = jsParser.parse();
        if (parseResult instanceof JSObject) {
          socketListener.onMessageReceived((JSObject) parseResult);
        } else {
          final String msg = "Unexpected message (expected JSObject): " +
              JSSerializer.stringify(parseResult);
          socketListener.onTransportError(new RuntimeException(msg));
        }
      } catch (JSParsingException e) {
        logger.info("Message parsing failed", e);
      }
    }
    messageBuilder.reset();
    if (b == -1) {
      logger.trace("Remote host closed connection (Input steam closed)");
      closeInternal();
    }
  }

  private boolean initConnection() throws IOException {
    final boolean connected;
    synchronized (TCPTransport.this) {
      if (socket == null || !socket.isConnected() || socket.isClosed()) {
        if (sslEnabled) {
          socket = createSSLSocket(host, port);
        } else {
          socket = new Socket(host, port);
        }
      }

      connected = socket != null && socket.isConnected();
      if (connected) {
        logger.trace("Created socket: {}:{}", host, port);
        running = true;
        out = socket.getOutputStream();
        in = new BufferedInputStream(socket.getInputStream());
      }
      connecting = false;
    }
    if (connected) {
      if (socketListener != null) {
        socketListener.onTransportConnected();
      }
      return true;
    }
    return false;
  }

  private Socket createSSLSocket(String host, int port) throws IOException {
    try {
      SSLContext context = SSLContext.getInstance("TLSv1.2");
      context.init(null, null, null);
      return context.getSocketFactory().createSocket(host, port);
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      logger.warn("Cannot create SSL socket", e);
    }
    return null;
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
    logger.trace("Public close transport");
    closing = true;
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          closing = true;
          if (!forced) {
            synchronized (this) {
              long delay = 0;
              while (delay <= DEFAULT_CLOSING_TIMEOUT && !messageQueue.isEmpty()) {
                wait(DEFAULT_CLOSING_TICK);
                delay += DEFAULT_CLOSING_TICK;
              }
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
    return !connecting && !closing && socket != null && socket.isConnected();
  }

  @Override
  public boolean isClosed() {
    return closing || socket == null || socket.isClosed();
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
}
