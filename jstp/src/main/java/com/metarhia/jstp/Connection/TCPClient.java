package com.metarhia.jstp.Connection;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Lida on 08.07.16.
 */
public class TCPClient {

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
    private TCPMessageReceiver messageReceiver;

    private OutputStreamWriter out;
    private BufferedReader in;

    private OnErrorListener errorListener;

    public TCPClient(String host, int port) {
       this(host, port, false);
    }

    public TCPClient(String host, int port, boolean sslEnabled) {
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
                        if (errorListener != null) errorListener.onNetworkError("Cannot connect", null);
                    }
                } catch (IOException e) {
                    if (errorListener != null) errorListener.onNetworkError("Cannot initialize connection", e);
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
                            out.write(message.toCharArray());
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
                    if (errorListener != null) errorListener.onNetworkError("Cannot send message", e);
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
                    while (!closing) {
                        while (running) {
                            while (in.ready()) {
                                sb.append((char) in.read());
                            }
                            if (sb.length() != 0 && messageReceiver != null) {
                                 System.out.println("com.metarhia.jstp.Connection: " + sb.toString());
                                // TODO add proper conditional logging
                                messageReceiver.onMessageReceived(sb.toString());
                                sb.delete(0, sb.length());
                            }
                            // little delay to ease the work of a scheduler
                            Thread.sleep(10);
                        }
                        synchronized (pauseLock) {
                            pauseLock.wait();
                        }
                    }
                } catch (InterruptedException | ClosedByInterruptException e) {
                    // all ok - manually closing
                } catch (IOException e) {
                    if (errorListener != null) errorListener.onNetworkError("Cannot receive message", e);
                }
            }
        });
        receiverThread.start();
    }

    private boolean initConnection(boolean useSSL) throws IOException {
        if (socket == null || !socket.isConnected() || socket.isClosed()) {
            InetAddress host = InetAddress.getByName(this.host);
            socket = new Socket(host, port);
        }

        if (socket.isConnected()) {
            if (sslEnabled) initSSL();
            if (!socket.isConnected()) return false;

            running = true;
            out = new OutputStreamWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return true;
        }
        return false;
    }

    private void initSSL() {
        try {
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, null, new SecureRandom());
            socket = context.getSocketFactory().createSocket(socket, host, port, false);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            receiverThread = null;
            senderThread = null;
            socket = null;
        } catch (IOException e) {
            if (errorListener != null) errorListener.onNetworkError("Cannot close connection", e);
        }
        closing = false;
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

    public void setMessageReceiver(TCPMessageReceiver receiver) {
        this.messageReceiver = receiver;
    }

    public void setErrorListener(OnErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public interface OnErrorListener {
        void onNetworkError(String message, Exception e);
    }

    public interface TCPMessageReceiver {
        void onMessageReceived(String message);
    }
}
