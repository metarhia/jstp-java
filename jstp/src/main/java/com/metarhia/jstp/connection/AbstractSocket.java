package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSTypes.JSObject;

public abstract class AbstractSocket {

    protected AbstractSocketListener socketListener;

    public AbstractSocket(AbstractSocketListener listener) {
        this.socketListener = listener;
    }

    /**
     * @return true if connect task was committed (it doesn't mean that connection was established)
     * else returns false if error occurred
     */
    public abstract boolean connect();

    public abstract void send(String message);

    public abstract void pause();

    public abstract void resume();

    public abstract void close(boolean forced);

    public abstract void clearQueue();

    public abstract int getQueueSize();

    public void setSocketListener(AbstractSocketListener listener) {
        this.socketListener = listener;
    }

    public abstract boolean isConnected();

    public abstract boolean isClosed();

    public abstract boolean isRunning();

    public interface AbstractSocketListener {
        void onConnected();

        void onPacketReceived(JSObject packet);

        void onConnectionClosed(int remainingMessages);

        void onMessageRejected(String message);

        void onError(Exception e);
    }
}