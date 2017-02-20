package com.metarhia.jstp.Connection;

import com.metarhia.jstp.Handlers.StateHandler;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSTypes.JSArray;
import com.metarhia.jstp.core.JSTypes.JSNumber;
import com.metarhia.jstp.core.JSTypes.JSObject;
import com.metarhia.jstp.core.JSTypes.JSValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JSTPConnection implements AbstractSocket.AbstractSocketListener {

    /**
     * Package terminator
     */
    public static final String TERMINATOR = "\0";

    public static final String STREAM_DATA = "data";

    // Package types
    private static final String HANDSHAKE = "handshake";
    private static final String CALL = "call";
    private static final String CALLBACK = "callback";
    private static final String EVENT = "event";
    private static final String STATE = "state";
    private static final String STREAM = "stream";
    private static final String INSPECT = "inspect";
    private static final String PING = "ping";
    private static final String PONG = "pong";

    private static final Map<String, Method> METHOD_HANDLERS = new HashMap<>(10);

    static {
        try {
            METHOD_HANDLERS.put(HANDSHAKE,
                    JSTPConnection.class.getDeclaredMethod("handshakePacketHandler", JSObject.class));
            METHOD_HANDLERS.put(CALL,
                    JSTPConnection.class.getDeclaredMethod("callPacketHandler", JSObject.class));
            METHOD_HANDLERS.put(CALLBACK,
                    JSTPConnection.class.getDeclaredMethod("callbackPacketHandler", JSObject.class));
            METHOD_HANDLERS.put(EVENT,
                    JSTPConnection.class.getDeclaredMethod("eventPacketHandler", JSObject.class));
            METHOD_HANDLERS.put(INSPECT,
                    JSTPConnection.class.getDeclaredMethod("inspectPacketHandler", JSObject.class));
            METHOD_HANDLERS.put(STATE,
                    JSTPConnection.class.getDeclaredMethod("statePacketHandler", JSObject.class));
            METHOD_HANDLERS.put(STREAM,
                    JSTPConnection.class.getDeclaredMethod("streamPacketHandler", JSObject.class));
            METHOD_HANDLERS.put(PING,
                    JSTPConnection.class.getDeclaredMethod("pingPacketHandler", JSObject.class));
            METHOD_HANDLERS.put(PONG,
                    JSTPConnection.class.getDeclaredMethod("pongPacketHandler", JSObject.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /**
     * Package counter
     */
    private int packageCounter = 0;

    /**
     * Transport to send/receive packets
     */
    private AbstractSocket transport;

    /**
     * Call handlers table. Handlers are associated with names of methods they handle
     */
    private Map<String, ManualHandler> callHandlers;

    /**
     * Event handlers table. Handlers are associated with names of interfaces they handle
     */
    private Map<String, Map<String, List<ManualHandler>>> eventHandlers;

    /**
     * Callback, stream and handshake handlers. Handlers are associated with numbers of
     * packages incoming to server.
     */
    private Map<Integer, ManualHandler> handlers;

    /**
     * State handler
     */
    private StateHandler stateHandler;

    /**
     * Client method names for incoming inspect packages
     */
    private JSArray clientMethodNames;

    private List<JSTPConnectionListener> connectionListeners;

    public JSTPConnection(String host, int port) {
        this(host, port, true);
    }

    /**
     * Creates new JSTP connection
     *
     * @param host       of the server
     * @param port       of the server
     * @param sslEnabled determines whether connection will use SSL or not (see {@link #setSSLEnabled})
     */
    public JSTPConnection(String host, int port, boolean sslEnabled) {
        resetConnection();
        createNewConnection(host, port, sslEnabled);
        connectionListeners = new ArrayList<>();
        eventHandlers = new ConcurrentHashMap<>();
        clientMethodNames = new JSArray();
        handlers = new ConcurrentHashMap<>();
        callHandlers = new ConcurrentHashMap<>();
    }

    private void resetConnection() {
        resetPackageCounter();
        handlers = new ConcurrentHashMap<>();
        callHandlers = new ConcurrentHashMap<>();
        if (transport != null) {
            transport.setSocketListener(null);
            transport = null;
        }
    }

    public void createNewConnection(AbstractSocket transport) {
        if (this.transport != null) this.transport.close();
        resetConnection();
        this.transport = transport;
        this.transport.setSocketListener(this);
    }

    public void createNewConnection(String host, int port, boolean sslEnabled) {
        createNewConnection(new TCPTransport(host, port, sslEnabled, this));
    }

    /**
     * Opens connection to the specified in constructor (host, port) (see {@link TCPTransport})
     * And sends handshake message through it
     *
     * @param applicationName name used during handshake
     * @param handler         optional handler that will be called when response handshake message comes from the
     *                        server
     */
    public void handshake(String applicationName, ManualHandler... handler) {
        int packageCounter = resetPackageCounter();
        if (handler.length != 0) {
            handlers.put(packageCounter, handler[0]);
        }

        JSTPMessage hm = new JSTPMessage(packageCounter, HANDSHAKE);
        hm.addProtocolArg(applicationName);

        transport.openConnection(hm.getMessage() + TERMINATOR);
    }

    public void setClientMethodNames(String... names) {
        for (String name : names) {
            clientMethodNames.add(name);
        }
    }

    public void inspect(String interfaceName, ManualHandler handler) {
        int packageCounter = nextPackageCounter();
        JSTPMessage inspectMessage = new JSTPMessage(packageCounter, INSPECT);
        inspectMessage.addProtocolArg(interfaceName);

        handlers.put(packageCounter, handler);

        transport.sendMessage(inspectMessage.getMessage() + TERMINATOR);
    }

    public void event(String interfaceName, String methodName, JSArray args) {
        int packageCounter = nextPackageCounter();
        JSTPMessage eventMessage = new JSTPMessage(packageCounter, EVENT, methodName, args);
        eventMessage.addProtocolArg(interfaceName);

        transport.sendMessage(eventMessage.getMessage() + TERMINATOR);
    }

    public void call(String interfaceName,
                     String methodName,
                     JSArray args,
                     ManualHandler... handler) {
        int packageCounter = nextPackageCounter();
        JSTPMessage callMessage = new JSTPMessage(packageCounter, CALL, methodName, args);
        callMessage.addProtocolArg(interfaceName);

        if (handler.length != 0) {
            handlers.put(packageCounter, handler[0]);
        }

        transport.sendMessage(callMessage.getMessage() + TERMINATOR);
    }

    public int nextPackageCounter() {
        return packageCounter++;
    }

    private int resetPackageCounter() {
        packageCounter = 0;
        return packageCounter++;
    }

    public void callback(JSCallback value, JSValue args) {
        callback(value, args, null);
    }

    public void callback(JSCallback value, JSValue args, Integer customPackageIndex) {
        int packageNumber = customPackageIndex == null ? nextPackageCounter() : customPackageIndex;

        JSTPMessage callbackMessage = new JSTPMessage(packageNumber, CALLBACK, value.toString(), args);

        transport.sendMessage(callbackMessage.getMessage() + TERMINATOR);
    }

    public void addSocketListener(JSTPConnectionListener listener) {
        this.connectionListeners.add(listener);
    }

    @Override
    public void onMessageReceived(JSObject packet) {
        try {
            List<String> keys = packet.getOrderedKeys();
            if (keys.size() == 0) {
                rejectPacket(packet);
                return;
            }

            final Method handler = METHOD_HANDLERS.get(keys.get(0));
            if (handler != null) {
                int receiverIndex = getPacketNumber(packet);
                packageCounter = packageCounter <= receiverIndex ? receiverIndex + 1 : packageCounter;

                handler.invoke(this, packet);
            } else {
                rejectPacket(packet);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void handshakePacketHandler(JSObject packet) {
        callbackPacketHandler(packet);
    }

    private void callPacketHandler(JSObject packet) {
        String methodName = packet.getOrderedKeys().get(1);
        ManualHandler handler = callHandlers.get(methodName);
        if (handler != null) handler.invoke(packet);
    }

    private void callbackPacketHandler(JSObject packet) {
        int receiverIndex = getPacketNumber(packet);
        ManualHandler callbackHandler = handlers.remove(receiverIndex);
        if (callbackHandler != null) callbackHandler.invoke(packet);
    }

    private void eventPacketHandler(JSObject packet) {
        String interfaceName = getInterfaceName(packet);
        Map<String, List<ManualHandler>> interfaceHandlers = eventHandlers.get(interfaceName);
        if (interfaceHandlers == null) return;

        String eventName = getEventName(packet);
        List<ManualHandler> eventHandlers = interfaceHandlers.get(eventName);
        if (eventHandlers == null) return;

        for (ManualHandler eh : eventHandlers) {
            eh.invoke(packet);
        }
    }

    private void inspectPacketHandler(JSObject packet) {
        callback(JSCallback.OK, clientMethodNames);
    }

    private void statePacketHandler(JSObject packet) {
        stateHandler.onState(packet);
    }

    private void pingPacketHandler(JSObject packet) {
        int pingNumber = getPacketNumber(packet);
        JSTPMessage streamMessage = new JSTPMessage(pingNumber, PONG);
        transport.sendMessage(streamMessage.getMessage() + TERMINATOR);
    }

    private void pongPacketHandler(JSObject packet) {
        callbackPacketHandler(packet);
    }

    private void streamPacketHandler(JSObject packet) {
        int receiverIndex = getPacketNumber(packet);
        ManualHandler responseHandler = handlers.get(receiverIndex);
        if (responseHandler != null) responseHandler.invoke(packet);
    }

    public int openStream(JSValue data) {
        int packageCounter = nextPackageCounter();
        JSTPMessage streamMessage = new JSTPMessage(packageCounter, STREAM, STREAM_DATA, data);
        transport.sendMessage(streamMessage.getMessage() + TERMINATOR);
        return packageCounter;
    }

    public void writeStream(int packageNumber, JSValue data) {
        JSTPMessage streamMessage = new JSTPMessage(packageNumber, STREAM, STREAM_DATA, data);
        transport.sendMessage(streamMessage.getMessage() + TERMINATOR);
    }

    public void addCallHandler(String methodName, ManualHandler callHandler) {
        callHandlers.put(methodName, callHandler);
    }

    public void setStateHandler(StateHandler stateHandler) {
        this.stateHandler = stateHandler;
    }

    public void addEventHandler(String interfaceName, String eventName, ManualHandler handler) {
        Map<String, List<ManualHandler>> ehs = eventHandlers.get(interfaceName);
        if (ehs == null) {
            eventHandlers.put(interfaceName, new HashMap<String, List<ManualHandler>>());
            ehs = eventHandlers.get(interfaceName);
            ehs.put(eventName, new ArrayList<ManualHandler>());
        }
        List<ManualHandler> eventHandlers = ehs.get(eventName);
        if (eventHandlers == null) {
            ehs.put(eventName, new ArrayList<ManualHandler>());
            eventHandlers = ehs.get(eventName);
        }
        eventHandlers.add(handler);
    }

    public void removeEventHandler(String interfaceName, String eventName, ManualHandler handler) {
        Map<String, List<ManualHandler>> ehs = eventHandlers.get(interfaceName);
        if (ehs != null) {
            List<ManualHandler> eventHandlers = ehs.get(eventName);
            if (eventHandlers != null) eventHandlers.remove(handler);
        }
    }

    public void addHandler(int packetIndex, ManualHandler manualHandler) {
        handlers.put(packetIndex, manualHandler);
    }

    public void close() {
        transport.close();
    }

    public void pause() {
        transport.pause();
    }

    public void pause(boolean clear) {
        transport.pause(clear);
    }

    public void resume() {
        transport.resume();
    }

    public void resume(boolean clear) {
        transport.resume(clear);
    }

    @Deprecated
    public void closeConnection() {
        close();
    }

    private String getInterfaceName(JSObject messageObject) {
        return (String) ((JSArray) messageObject.get(EVENT))
                .get(1)
                .getGeneralizedValue();
    }

    private String getEventName(JSObject messageObject) {
        return messageObject.getOrderedKeys().get(1);
    }

    private int getPacketNumber(JSObject messageObject) {
        return (int) ((JSNumber) ((JSArray) messageObject.get(0))
                .get(0))
                .getValue();
    }

    public boolean isSSLEnabled() {
        return transport.isSSLEnabled();
    }

    /**
     * <p>Determines whether connection will use SSL or not</p>
     * <p>
     * <p>Must be set before calling {@link #handshake(String, ManualHandler...)}
     * as it will open the connection</p>
     * <p>
     * <p>To apply the changed value (after handshake has been done) tcp connection must be
     * restarted: call {@link #close()} and then {@link #handshake(String, ManualHandler...)}
     * again to create connection with new settings</p>
     *
     * @param sslEnabled If true then connection will be opened with ssl enabled else ssl will not be used
     */
    public void setSSLEnabled(boolean sslEnabled) {
        transport.setSSLEnabled(sslEnabled);
    }

    public String getHost() {
        return transport.getHost();
    }

    /**
     * <p>Set address of the server</p>
     * <p>
     * <p>Must be set before calling {@link #handshake(String, ManualHandler...)}
     * as it will open the connection</p>
     * <p>
     * <p>To apply the changed value (after handshake has been done) tcp connection must be
     * restarted: call {@link #close()} and then {@link #handshake(String, ManualHandler...)}
     * again to create connection with new settings</p>
     *
     * @param host address of the server (see {@link java.net.InetAddress#getByName(String)})
     */
    public void setHost(String host) {
        transport.setHost(host);
    }

    public int getPort() {
        return transport.getPort();
    }

    /**
     * <p>Set port number to connect to on the server</p>
     * <p>
     * <p>Must be set before calling {@link #handshake(String, ManualHandler...)}
     * as it will open the connection</p>
     * <p>
     * <p>To apply the changed value (after handshake has been done) tcp connection must be
     * restarted: call {@link #close()} and then {@link #handshake(String, ManualHandler...)}
     * again to create connection with new settings</p>
     *
     * @param port the port number
     */
    public void setPort(int port) {
        transport.setPort(port);
    }

    public boolean isConnected() {
        return transport.isConnected();
    }

    public boolean isClosed() {
        return transport.isClosed();
    }

    @Override
    public void onConnect() {
        for (JSTPConnectionListener listener : connectionListeners) listener.onConnect();
    }

    @Override
    public void onMessageRejected(String message) {
        // ignore for now (log later)
    }

    @Override
    public void onConnectionClosed(Exception... e) {
        resetConnection();
        for (JSTPConnectionListener listener : connectionListeners) listener.onConnectionClosed();
    }

    private void rejectPacket(JSObject packet) {
        for (JSTPConnectionListener listener : connectionListeners) listener.onPacketRejected(packet);
    }

    public interface JSTPConnectionListener {
        void onConnect();

        void onPacketRejected(JSObject packet);

        void onConnectionClosed();
    }
}