package com.metarhia.jstp.Connection;

/**
 * Created by lidaamber on 16.11.16.
 */
public abstract class AbstractSocket {

    private AbstractSocketListener socketListener;

    public AbstractSocket(AbstractSocketListener listener) {
        this.socketListener = listener;
    }

    public abstract void openConnection(String handshakeMessage);

    public abstract void sendMessage(String message);

    public abstract void pause();

    public abstract void pause(boolean clear);

    public abstract void resume();

    public abstract void resume(boolean clear);

    public abstract void close();

    public abstract void setHost(String host);

    public abstract void setPort(int port);

    public abstract void setSSLEnabled(boolean enabled);

    public abstract String getHost();

    public abstract int getPort();

    public abstract boolean isSSLEnabled();

    public void setSocketListener(AbstractSocketListener listener) {
        this.socketListener = listener;
    }

    protected AbstractSocketListener getSocketListener() {
        return this.socketListener;
    }

    public abstract boolean isConnected();

    public abstract boolean isClosed();

    public interface AbstractSocketListener {
        void onConnect();

        void onMessageReceived(String message);

        void onConnectionClosed(Exception ... e);
    }
}