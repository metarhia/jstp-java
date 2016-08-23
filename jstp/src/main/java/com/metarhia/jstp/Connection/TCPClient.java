package com.metarhia.jstp.Connection;

import com.metarhia.jstp.core.JSTypes.JSValue;

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

import javax.net.ssl.SSLContext;

/**
 * Created by Lida on 08.07.16.
 */
public class TCPClient {

    private String host;
    private int port;

    private boolean running;
    private Thread receiverThread;
    private Thread senderThread;
    private ConcurrentLinkedQueue<String> messageQueue;
    private final Object senderLock = new Object();

    private Socket socket;
    private TCPMessageReceiver messageReceiver;

    private OutputStreamWriter out;
    private BufferedReader in;

    public TCPClient(String host, int port) {
        this.host = host;
        this.port = port;
        messageQueue = new ConcurrentLinkedQueue<>();
    }

    public void openConnection(final JSValue handshakeMessage, final String terminator) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (initConnection()) {
                        startReceiverThread();
                        startSenderThread();

                        sendMessage(handshakeMessage + terminator);
                    } else {
                        // todo error reporting
                    }
                } catch (IOException e) {
                    // TODO error reporting
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startSenderThread() {
        senderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (running) {
                        synchronized (senderLock) {
                            if (messageQueue.isEmpty()) {
                                senderLock.wait();
                            }
                        }
                        String message = messageQueue.poll();
                        out.write(message.toCharArray());
                        out.flush();
                    }
                } catch (InterruptedException | ClosedByInterruptException e) {
                    // todo error handling
                    // all ok - manually closing
                    e.printStackTrace();
                } catch (IOException e) {
                    // todo error handling
                    e.printStackTrace();
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
                    while (running) {
                        while (in.ready()) {
                            sb.append((char) in.read());
                        }
                        if (sb.length() != 0 && messageReceiver != null) {
                            System.out.println("NYAAAAAAAAAAAAAAAA: " + sb.toString());
                            messageReceiver.onMessageReceived(sb.toString());
                            sb.delete(0, sb.length());
                        }
                        // little delay to ease the work of a scheduler
                        Thread.sleep(10);
                    }
                    TCPClient.this.stop();
                } catch (InterruptedException | ClosedByInterruptException e) {
                    // todo error reporting
                    // all ok - manually closing
                } catch (IOException e) {
                    // todo error reporting
                    e.printStackTrace();
                }
            }
        });
        receiverThread.start();
    }

    private boolean initConnection() throws IOException {
        if (socket == null || !socket.isConnected() || socket.isClosed()) {
            InetAddress host = InetAddress.getByName(this.host);
            socket = new Socket(host, port);

            // TODO use it when TLS will be deployed to server
            //initSSL();

        }

        if (socket.isConnected()) {
            running = true;
            out = new OutputStreamWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return true;
        }
        return false;
    }

    private void initSSL() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");

            //TODO init with key manager and trust manager
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

    public void setMessageReceiver(TCPMessageReceiver receiver) {
        this.messageReceiver = receiver;
    }

    public void sendMessage(String message) {
        System.out.println("NYAAAAAAAAAAAAAAAA: " + message);
        messageQueue.add(message);
        synchronized (senderLock) {
            senderLock.notify();
        }
    }

    public void stop() {
        stop(false);
    }

    public void stop(boolean force) {
        this.running = false;
        if (force) messageQueue.clear();
    }

    public void close() {
        try {
            stop(true);
            receiverThread.interrupt();
            senderThread.interrupt();
            if (in != null) {
                in.close();
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            // todo error erporting
            e.printStackTrace();
        }
    }

    public static abstract class TCPMessageReceiver {
        protected abstract void onMessageReceived(String message);
    }
}
