package com.metarhia.jstp.Connection;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Lida on 08.07.16.
 */
public class TCPClient extends AbstractSocket {

    private String host;
    private int port;
    private boolean sslEnabled;

    private boolean running;
    private boolean closing;
    private Thread receiverThread;
    private Thread senderThread;
    private ConcurrentLinkedQueue<String> messageQueue;
    private final Object senderLock = new Object();
    private final Object pauseLock = new Object();

    private Socket socket;

    private OutputStream out;
    private InputStream in;

    public TCPClient(String host, int port, AbstractSocket.AbstractSocketListener listener) {
        this(host, port, false, listener);
    }

    public TCPClient(String host, int port, boolean sslEnabled, AbstractSocket.AbstractSocketListener listener) {
        super(listener);

        this.host = host;
        this.port = port;
        this.sslEnabled = sslEnabled;
        messageQueue = new ConcurrentLinkedQueue<>();
    }

    public void openConnection(final String handshakeMessage) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (initConnection(sslEnabled)) {
                        startReceiverThread();
                        startSenderThread();

                        sendMessage(handshakeMessage);
                    } else {
                        getSocketListener().onConnectionFailed();
                    }
                } catch (IOException e) {
                    getSocketListener().onConnectionClosed(e);
                }
            }
        }).start();
    }

    private void startSenderThread() {
        senderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!closing) {
                        while (running) {
                            synchronized (senderLock) {
                                if (messageQueue.isEmpty()) {
                                    senderLock.wait();
                                }
                            }
                            // TODO add proper conditional logging
                            String message = messageQueue.poll();
                            System.out.println("com.metarhia.jstp.Connection: " + message);
                            out.write(message.getBytes());
                            out.write(0);
                            out.flush();
                        }
                        synchronized (pauseLock) {
                            pauseLock.wait();
                        }
                    }
                } catch (InterruptedException | ClosedByInterruptException e) {
                    // all ok - manually closing
                    e.printStackTrace();
                } catch (IOException e) {
                    getSocketListener().onConnectionClosed(e);
                }
            }
        });
        senderThread.start();
    }

    private void startReceiverThread() {
        receiverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StringBuilder sb = new StringBuilder();
                    int b = -1;
                    while (!closing) {
                        while (running) {
                            while ((b = in.read()) > 0) {
                                sb.append((char) b);
                            }
                            if (sb.length() != 0 && getSocketListener() != null) {
                                sb.append('\0');
                                System.out.println("com.metarhia.jstp.Connection: " + sb.toString());
                                // TODO add proper conditional logging
                                getSocketListener().onMessageReceived(sb.toString());
                                sb.delete(0, sb.length());
                            }
                            if (b == -1) {
                                close();
                            }
                            // little delay to ease the work of a scheduler
//                            Thread.sleep(10);
                        }
                        synchronized (pauseLock) {
                            pauseLock.wait();
                        }
                    }
                } catch (InterruptedException | ClosedByInterruptException e) {
                    // all ok - manually closing
                } catch (IOException e) {
                    getSocketListener().onConnectionClosed(e);
                }
            }
        });
        receiverThread.start();
    }

    private boolean initConnection(boolean useSSL) throws IOException {
        if (socket == null || !socket.isConnected() || socket.isClosed()) {
            if (sslEnabled) socket = createSSLSocket(host, port);
            else socket = new Socket(host, port);
        }

        if (socket != null && socket.isConnected()) {
            running = true;
            out = socket.getOutputStream();
            in = socket.getInputStream();
            getSocketListener().onConnect();
            return true;
        }
        return false;
    }

    private Socket createSSLSocket(String host, int port) {
        try {
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, null, null);
            return context.getSocketFactory().createSocket(host, port);
//            return SSLContext.getDefault().getSocketFactory().createSocket(host, port);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendMessage(String message) {
        messageQueue.add(message);
        if (running) {
            synchronized (senderLock) {
                senderLock.notify();
            }
        }
    }

    public void pause() {
        pause(false);
    }

    public void pause(boolean clear) {
        this.running = false;
        if (clear) messageQueue.clear();
    }

    public void resume() {
        resume(false);
    }

    public void resume(boolean clear) {
        running = true;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
        if (!clear && !messageQueue.isEmpty()) {
            synchronized (senderLock) {
                senderLock.notify();
            }
        } else {
            messageQueue.clear();
        }
    }

    public void close() {
        try {
            pause(true);
            closing = true;
            if (receiverThread != null) receiverThread.interrupt();
            if (senderThread != null) senderThread.interrupt();
            if (in != null) in.close();
            if (socket != null) socket.close();
            in = null;
            receiverThread = null;
            senderThread = null;
            socket = null;
            getSocketListener().onConnectionClosed();
        } catch (IOException e) {
            getSocketListener().onConnectionClosed(e);
        }
        closing = false;
    }

    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    @Override
    public boolean isClosed() {
        return socket == null || socket.isClosed();
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
}
