package com.metarhia.jstp.Connection;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TCPTransport extends AbstractSocket {

    private final Object senderLock = new Object();
    private final Object pauseLock = new Object();
    private String host;
    private int port;
    private boolean sslEnabled;
    private boolean running;
    private boolean closing;
    private Thread receiverThread;
    private Thread senderThread;
    private ConcurrentLinkedQueue<String> messageQueue;
    private Socket socket;

    private OutputStream out;
    private BufferedInputStream in;

    private ByteArrayOutputStream packetBuilder;

    public TCPTransport(String host, int port) {
        this(host, port, null);
    }

    public TCPTransport(String host, int port, AbstractSocket.AbstractSocketListener listener) {
        this(host, port, false, listener);
    }

    public TCPTransport(String host, int port, boolean sslEnabled) {
        this(host, port, sslEnabled, null);
    }

    public TCPTransport(String host, int port, boolean sslEnabled, AbstractSocket.AbstractSocketListener listener) {
        super(listener);

        this.host = host;
        this.port = port;
        this.sslEnabled = sslEnabled;
        packetBuilder = new ByteArrayOutputStream(100);
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
                        close();
                    }
                } catch (IOException e) {
                    close();
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
                                while (messageQueue.isEmpty()) {
                                    senderLock.wait();
                                }
                            }
                            sendMessageInternal(messageQueue.poll());
                        }
                        synchronized (pauseLock) {
                            pauseLock.wait();
                        }
                    }
                } catch (InterruptedException | ClosedByInterruptException e) {
                    // all ok - manually closing
                    e.printStackTrace();
                } catch (IOException e) {
                    close();
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
                        while (running) processMessage();
                        synchronized (pauseLock) {
                            pauseLock.wait();
                        }
                    }
                } catch (InterruptedException | ClosedByInterruptException e) {
                    // all ok - manually closing
                } catch (IOException e) {
                    close();
                }
            }
        });
        receiverThread.start();
    }

    private void processMessage() throws IOException {
        int b;
        while ((b = in.read()) > 0) packetBuilder.write(b);

        if (packetBuilder.size() != 0 && socketListener != null) {
            packetBuilder.write('\0');
            String message = packetBuilder.toString();
//            System.out.println("com.metarhia.jstp.Connection: " + message);
            // TODO add proper conditional logging
            socketListener.onMessageReceived(message);
        }
        packetBuilder.reset();
        if (b == -1) close();
    }

    private boolean initConnection(boolean useSSL) throws IOException {
        if (socket == null || !socket.isConnected() || socket.isClosed()) {
            if (sslEnabled) socket = createSSLSocket(host, port);
            else socket = new Socket(host, port);
        }

        if (socket != null && socket.isConnected()) {
            running = true;
            out = socket.getOutputStream();
            in = new BufferedInputStream(socket.getInputStream());
            if (socketListener != null) socketListener.onConnect();
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
            if (socketListener != null) socketListener.onConnectionClosed();
        } catch (IOException e) {
            if (socketListener != null) socketListener.onConnectionClosed(e);
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
