package com.metarhia.jstp.transport;

import com.metarhia.jstp.connection.AbstractSocket;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSParsingException;
import com.metarhia.jstp.core.JSTypes.JSObject;
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

public class TCPTransport extends AbstractSocket {

  public static final long DEFAULT_CLOSING_TICK = 1000;
  public static final long DEFAULT_CLOSING_TIMEOUT = 5000;
  public static final int DEFAULT_PACKET_SIZE = 100;

  private final Object senderLock = new Object();
  private final Object pauseLock = new Object();
  private String host;
  private int port;
  private boolean sslEnabled;
  private boolean running;
  private boolean closing;
  private Thread receiverThread;
  private Thread senderThread;
  private Queue<String> messageQueue;
  private Socket socket;

  private OutputStream out;
  private BufferedInputStream in;

  private JSParser jsParser;
  private ByteArrayOutputStream packetBuilder;

  private long closingTick;
  private long closingTimeout;

  public TCPTransport(String host, int port) {
    this(host, port, null);
  }

  public TCPTransport(String host, int port, AbstractSocket.AbstractSocketListener listener) {
    this(host, port, false, listener);
  }

  public TCPTransport(String host, int port, boolean sslEnabled) {
    this(host, port, sslEnabled, null);
  }

  public TCPTransport(String host, int port, boolean sslEnabled,
      AbstractSocket.AbstractSocketListener listener) {
    super(listener);

    closingTick = DEFAULT_CLOSING_TICK;
    closingTimeout = DEFAULT_CLOSING_TIMEOUT;

    this.host = host;
    this.port = port;
    this.sslEnabled = sslEnabled;
    packetBuilder = new ByteArrayOutputStream(DEFAULT_PACKET_SIZE);
    messageQueue = new ConcurrentLinkedQueue<>();
    jsParser = new JSParser();
  }

  public boolean connect() {
    if (isConnected()) {
      if (socketListener != null) {
        socketListener.onError(new AlreadyConnectedException());
      }
      return false;
    }
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
          closeInternal();
        }
      }
    }).start();
    return true;
  }

  private void startSenderThread() {
    senderThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          String message;
          while (!closing) {
            while (running) {
              synchronized (senderLock) {
                while (messageQueue.isEmpty()) {
                  senderLock.wait();
                }
              }
              message = messageQueue.peek();
              sendMessageInternal(message);
              messageQueue.poll();
            }
            synchronized (pauseLock) {
              pauseLock.wait();
            }
          }
        } catch (InterruptedException | ClosedByInterruptException e) {
          // all ok - manually closing
        } catch (IOException e) {
          closeInternal();
        }
      }
    });
    senderThread.start();
  }

  private void sendMessageInternal(String message) throws IOException {
    // TODO add proper conditional logging
//        System.out.println("com.metarhia.jstp.Connection: " + message);
    out.write(message.getBytes());
    out.write(0);
    out.flush();
  }

  private void startReceiverThread() {
    receiverThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          while (!closing) {
            while (running) {
              processMessage();
            }
            synchronized (pauseLock) {
              pauseLock.wait();
            }
          }
        } catch (InterruptedException | ClosedByInterruptException e) {
          // all ok - manually closing
        } catch (IOException e) {
          closeInternal();
        }
      }
    });
    receiverThread.start();
  }

//    private void processMessage() throws IOException {
//        for (int i = 0; i < 1024; ++i) packetData[i] = -1;
//        length[0] = in.read(packetData, length[0], packetData.length - length[0]) + length[0];
//        if (in.available() >= 0) {
//            packetData[length[0]] = (byte) in.read();
//            length[0] += 1;
//        }
//        if (length[0] == -1) close();
//        else if (socketListener != null) {
//            try {
//                List<JSObject> packets = JSNetworkParser.parse(packetData, length);
//                for (JSObject packet : packets) socketListener.onPacketReceived(packet);
//            } catch (JSParsingException e) {
//                socketListener.onMessageRejected(null);
//            }
//        }
//    }

  void processMessage() throws IOException {
    int b;
    while ((b = in.read()) > 0) {
      packetBuilder.write(b);
    }

    if (packetBuilder.size() != 0 && socketListener != null) {
      packetBuilder.write('\0');
      String message = packetBuilder.toString();
//            System.out.println("com.metarhia.jstp.Connection: " + message);
//            TODO add proper conditional logging
      jsParser.setInput(message);
      try {
        JSObject packet = jsParser.parseObject();
        socketListener.onPacketReceived(packet);
      } catch (JSParsingException e) {
        socketListener.onMessageRejected(message);
      }
    }
    packetBuilder.reset();
    if (b == -1) {
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
      running = true;
      out = socket.getOutputStream();
      in = new BufferedInputStream(socket.getInputStream());
      if (socketListener != null) {
        socketListener.onConnected();
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
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (KeyManagementException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void send(String message) {
    if (closing) {
      if (socketListener != null) {
        socketListener.onMessageRejected(message);
      }
      return;
    }
    messageQueue.add(message);
    if (running) {
      synchronized (senderLock) {
        senderLock.notify();
      }
    }
  }

  public void clearQueue() {
    messageQueue.clear();
  }

  @Override
  public int getQueueSize() {
    return messageQueue.size();
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

  public void close(final boolean forced) {
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
    int remainingMessages;
    try {
      synchronized (TCPTransport.this) {
        remainingMessages = messageQueue.size();
        closing = true;
        clearQueue();
        running = false;
        if (receiverThread != null) {
          receiverThread.interrupt();
        }
        if (senderThread != null) {
          senderThread.interrupt();
        }
        if (in != null) {
          in.close();
        }
        if (socket != null) {
          socket.close();
        }
        in = null;
        receiverThread = null;
        senderThread = null;
        socket = null;
        closing = false;
      }
      if (notify && socketListener != null) {
        socketListener.onConnectionClosed(remainingMessages);
      }
    } catch (IOException e) {
    }
  }

  @Override
  public boolean isConnected() {
    return !closing && socket != null && socket.isConnected();
  }

  @Override
  public boolean isClosed() {
    return closing || socket == null || socket.isClosed();
  }

  @Override
  public boolean isRunning() {
    return isConnected() && running;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public boolean isSSLEnabled() {
    return sslEnabled;
  }

  public void setSSLEnabled(boolean sslEnabled) {
    this.sslEnabled = sslEnabled;
  }

  public long getClosingTick() {
    return closingTick;
  }

  public void setClosingTick(long closingTick) {
    this.closingTick = closingTick;
  }

  public long getClosingTimeout() {
    return closingTimeout;
  }

  public void setClosingTimeout(long closingTimeout) {
    this.closingTimeout = closingTimeout;
  }
}
