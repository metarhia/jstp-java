package com.metarhia.jstp.Connection;

import com.metarhia.jstp.Handlers.StateHandler;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSParsingException;
import com.metarhia.jstp.core.JSTypes.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Lida on 27.06.16.
 */
public class JSTPConnection implements
    TCPClient.TCPMessageReceiver,
    TCPClient.OnErrorListener {

    // Package types
    private static final String HANDSHAKE = "handshake";
    private static final String CALL = "call";
    private static final String CALLBACK = "callback";
    private static final String EVENT = "event";
    private static final String STATE = "state";
    private static final String STREAM = "stream";
    private static final String INSPECT = "inspect";

    /**
     * Package terminator
     */
    public static final String TERMINATOR = "\0";

    public static final String STREAM_DATA = "data";

    /**
     * Package counter
     */
    private int packageCounter = 0;

    /**
     * TCP client
     */
    private TCPClient tcpClient;

    /**
     * Call handlers table. Handlers are associated with names of methods they handle
     */
    private HashMap<String, ManualHandler> callHandlers;

    /**
     * Event handlers table. Handlers are associated with names of interfaces they handle
     */
    private HashMap<String, List<ManualHandler>> eventHandlers;

    /**
     * Callback, stream and handshake handlers. Handlers are associated with numbers of
     * packages incoming to server.
     */
    private HashMap<Integer, ManualHandler> handlers;

    /**
     * State handler
     */
    private StateHandler stateHandler;

    /**
     * Container used to store packet data until it can be parsed with {@link JSParser}
     * (until {@link #TERMINATOR} is not present)
     */
    private StringBuilder messageBuilder;

    /**
     * Client method names for incoming inspect packages
     */
    private JSArray clientMethodNames;

    /**
     * Listener that is used to report errors
     * (note that network errors may come from different thread)
     */
    private OnErrorListener errorListener;

    public JSTPConnection(String host, int port) {
        this(host, port, false);
    }

    /**
     * Creates new JSTP connection
     *
     * @param host       of the server
     * @param port       of the server
     * @param sslEnabled determines whether connection will use SSL or not (see {@link #setSSLEnabled})
     */
    public JSTPConnection(String host, int port, boolean sslEnabled) {
        tcpClient = new TCPClient(host, port, sslEnabled);
        tcpClient.setErrorListener(this);
        tcpClient.setMessageReceiver(this);
        handlers = new HashMap<>();
        eventHandlers = new HashMap<>();
        callHandlers = new HashMap<>();
        messageBuilder = new StringBuilder();
        clientMethodNames = new JSArray();
    }

    /**
     * Opens connection to the specified in constructor (host, port) (see {@link TCPClient})
     * And sends handshake message through it
     *
     * @param applicationName name used during handshake
     * @param handler         optional handler that will be called when response handshake message comes from the
     *                        server
     */
    public void handshake(String applicationName, ManualHandler... handler) {
        if (handler.length != 0) {
            handlers.put(packageCounter, handler[0]);
        }

        JSTPMessage hm = new JSTPMessage(packageCounter, HANDSHAKE);
        hm.addProtocolArg(applicationName);

        tcpClient.openConnection(hm.getMessage() + TERMINATOR);
        packageCounter++;
    }

    public void setClientMethodNames(String... names) {
        for (String name : names) {
            clientMethodNames.add(name);
        }
    }

    public void inspect(String interfaceName, ManualHandler handler) {
        JSTPMessage inspectMessage = new JSTPMessage(packageCounter, INSPECT);
        inspectMessage.addProtocolArg(interfaceName);

        handlers.put(packageCounter, handler);
        packageCounter++;

        tcpClient.sendMessage(inspectMessage.getMessage() + TERMINATOR);
    }

    public void event(String interfaceName, String methodName, JSArray args) {
        JSTPMessage eventMessage = new JSTPMessage(packageCounter, EVENT, methodName, args);
        eventMessage.addProtocolArg(interfaceName);

        packageCounter++;

        tcpClient.sendMessage(eventMessage.getMessage() + TERMINATOR);
    }

    public void call(String interfaceName,
                     String methodName,
                     JSArray args,
                     ManualHandler... handler) {

        JSTPMessage callMessage = new JSTPMessage(packageCounter, CALL, methodName, args);
        callMessage.addProtocolArg(interfaceName);

        if (handler.length != 0) {
            handlers.put(packageCounter, handler[0]);
        }

        packageCounter++;

        tcpClient.sendMessage(callMessage.getMessage() + TERMINATOR);
    }

    public void callback(JSCallback value, JSValue args) {
        callback(value, args, null);
    }

    public void callback(JSCallback value, JSValue args, Integer customPackageIndex) {
        int packageNumber = customPackageIndex == null ? packageCounter++ : customPackageIndex;

        JSTPMessage callbackMessage = new JSTPMessage(packageNumber, CALLBACK, value.toString(), args);

        tcpClient.sendMessage(callbackMessage.getMessage() + TERMINATOR);
    }

    @Override
    public void onNetworkError(String message, Exception e) {
        if (errorListener != null) {
            JSTPConnectionException connectionException = new JSTPConnectionException(message, e);
            errorListener.onNetworkError(connectionException);
        }
    }

    @Override
    public void onMessageReceived(String received) {
        messageBuilder.append(received);

        int startMessageIndex = 0;
        int endMessageIndex = messageBuilder.indexOf(TERMINATOR);
        while (endMessageIndex != -1) {
            String msg = messageBuilder.substring(startMessageIndex, endMessageIndex);
            int receiverIndex;

            try {
                JSValue parsed = new JSParser(msg).parse();
                if (!(parsed instanceof JSObject)) continue;
                JSObject messageObject = (JSObject) parsed;

                List<String> keys = messageObject.getOrderedKeys();
                switch (keys.get(0)) {
                    case CALLBACK:
                    case HANDSHAKE:
                    case STREAM:
                        receiverIndex = getPacketIndex(messageObject, keys.get(0));
                        ManualHandler responseHandler = handlers.get(receiverIndex);
                        if (responseHandler != null) {
                            responseHandler.invoke(messageObject);
                        }
                        break;
                    case CALL:
                        String methodName = keys.get(1);
                        ManualHandler handler = callHandlers.get(methodName);
                        if (handler != null) {
                            handler.invoke(messageObject);
                        }
                        packageCounter++;
                        break;
                    case EVENT:
                        String interfaceName = getInterfaceName(messageObject);
                        final List<ManualHandler> handlers = eventHandlers.get(interfaceName);
                        if (handlers != null) {
                            for (ManualHandler eh : handlers) {
                                eh.invoke(messageObject);
                            }
                        }
                        packageCounter++;
                        break;
                    case STATE:
                        stateHandler.onState(messageObject);
                        break;
                    case INSPECT:
                        handleInspect();
                        packageCounter++;
                        break;
                    default:
                        break;
                }
            } catch (JSParsingException e) {
                if (errorListener != null) errorListener.onParsingError(e);
            } finally {
                startMessageIndex = endMessageIndex + TERMINATOR.length();
                if (startMessageIndex == messageBuilder.length()) endMessageIndex = -1;
                else endMessageIndex = messageBuilder.indexOf(TERMINATOR, startMessageIndex);
            }
        }
        if (startMessageIndex != 0) messageBuilder.delete(0, startMessageIndex);
    }

    private void handleInspect() {
        callback(JSCallback.OK, clientMethodNames);
    }

    public int openStream(JSValue data) {
        JSTPMessage streamMessage = new JSTPMessage(packageCounter, STREAM, STREAM_DATA, data);
        tcpClient.sendMessage(streamMessage.getMessage() + TERMINATOR);

        return packageCounter++;
    }

    public void writeStream(int packageNumber, JSValue data) {
        JSTPMessage streamMessage = new JSTPMessage(packageNumber, STREAM, STREAM_DATA, data);
        tcpClient.sendMessage(streamMessage.getMessage() + TERMINATOR);
    }

    public void addCallHandler(String methodName, ManualHandler callHandler) {
        callHandlers.put(methodName, callHandler);
    }

    public void setStateHandler(StateHandler stateHandler) {
        this.stateHandler = stateHandler;
    }

    public void addEventHandler(String eventInterfaceName, ManualHandler handler) {
        List<ManualHandler> ehs = eventHandlers.get(eventInterfaceName);
        if (ehs == null) {
            eventHandlers.put(eventInterfaceName, new LinkedList<ManualHandler>());
            ehs = eventHandlers.get(eventInterfaceName);
        }
        ehs.add(handler);
    }

    public void addHandler(int packetIndex, ManualHandler manualHandler) {
        handlers.put(packetIndex, manualHandler);
    }

    public void removeEventHandler(String eventInterfaceName, ManualHandler handler) {
        List<ManualHandler> ehs = eventHandlers.get(eventInterfaceName);
        ehs.remove(handler);
    }

    @Deprecated
    public void close() {
        closeConnection();
    }

    public void pause() {
        tcpClient.pause();
    }

    public void pause(boolean clear) {
        tcpClient.pause(clear);
    }

    public void resume() {
        tcpClient.resume();
    }

    public void resume(boolean clear) {
        tcpClient.resume(clear);
    }

    public void closeConnection() {
        packageCounter = 0;
        tcpClient.close();
    }

    private String getInterfaceName(JSObject messageObject) {
        return (String) ((JSArray) messageObject.get(EVENT))
            .get(1)
            .getGeneralizedValue();
    }

    private int getPacketIndex(JSObject messageObject, String packageValue) {
        return (int) ((JSNumber) ((JSArray) messageObject.get(packageValue))
            .get(0))
            .getValue();
    }

    /**
     * <p>Determines whether connection will use SSL or not</p>
     *
     * <p>Must be set before calling {@link #handshake(String, ManualHandler...)}
     * as it will open the connection</p>
     *
     * <p>To apply the changed value (after handshake has been done) tcp connection must be
     * restarted: call {@link #close()} and then {@link #handshake(String, ManualHandler...)}
     * again to create connection with new settings</p>
     *
     * @param sslEnabled If true then connection will be opened with ssl enabled else ssl will not be used
     */
    public void setSSLEnabled(boolean sslEnabled) {
        tcpClient.setSSLEnabled(sslEnabled);
    }

    public boolean isSSLEnabled() {
        return tcpClient.isSSLEnabled();
    }

    public String getHost() {
        return tcpClient.getHost();
    }

    /**
     * <p>Set address of the server</p>
     *
     * <p>Must be set before calling {@link #handshake(String, ManualHandler...)}
     * as it will open the connection</p>
     *
     * <p>To apply the changed value (after handshake has been done) tcp connection must be
     * restarted: call {@link #close()} and then {@link #handshake(String, ManualHandler...)}
     * again to create connection with new settings</p>
     *
     * @param host address of the server (see {@link java.net.InetAddress#getByName(String)})
     */
    public void setHost(String host) {
        tcpClient.setHost(host);
    }

    public int getPort() {
        return tcpClient.getPort();
    }

    /**
     * <p>Set port number to connect to on the server</p>
     *
     * <p>Must be set before calling {@link #handshake(String, ManualHandler...)}
     * as it will open the connection</p>
     *
     * <p>To apply the changed value (after handshake has been done) tcp connection must be
     * restarted: call {@link #close()} and then {@link #handshake(String, ManualHandler...)}
     * again to create connection with new settings</p>
     *
     * @param port the port number
     */
    public void setPort(int port) {
        tcpClient.setPort(port);
    }

    public void setErrorListener(OnErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public interface OnErrorListener {
        void onNetworkError(JSTPConnectionException e);

        void onParsingError(JSParsingException e);
    }
}