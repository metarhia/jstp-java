package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSTypes.JSObject;

public abstract class AbstractSocket {

    protected AbstractSocketListener socketListener;

    public AbstractSocket(AbstractSocketListener listener) {
        this.socketListener = listener;
    }

    public abstract void openConnection();

    public abstract void sendMessage(String message);

    public abstract void pause();

    public abstract void resume();

    public abstract void close(boolean forced);

    public abstract void clearQueue();

    public void setSocketListener(AbstractSocketListener listener) {
        this.socketListener = listener;
    }

    public abstract boolean isConnected();

    public abstract boolean isClosed();

    public interface AbstractSocketListener {
        void onConnected();

        void onMessageReceived(JSObject packet);

        void onConnectionClosed(Exception... e);

        void onMessageRejected(String message);
    }
}