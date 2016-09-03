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
    public static final String TERMINATOR = ",{\f},";
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
     * Builder used to make up messages while terminator isn't present in incoming segment
     */
    private StringBuilder messageBuilder;

    /**
     * Client method names for incoming inspect packages
     */
    private JSArray clientMethodNames;

    private OnErrorListener errorListener;

    public JSTPConnection(String host, int port) {
        tcpClient = new TCPClient(host, port);
        tcpClient.setErrorListener(this);
        tcpClient.setMessageReceiver(this);
        handlers = new HashMap<>();
        eventHandlers = new HashMap<>();
        callHandlers = new HashMap<>();
        messageBuilder = new StringBuilder();
        clientMethodNames = new JSArray();
    }

    public void handshake(String applicationName, boolean useSSL, ManualHandler... handler) {
        if (handler.length != 0) {
            handlers.put(packageCounter, handler[0]);
        }

        JSTPMessage hm = new JSTPMessage(packageCounter, HANDSHAKE);
        hm.addProtocolArg(applicationName);

        tcpClient.openConnection(hm.getMessage() + TERMINATOR, useSSL);
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
                e.printStackTrace();
            } finally {
                startMessageIndex = endMessageIndex + TERMINATOR.length();
                endMessageIndex = messageBuilder.indexOf(TERMINATOR, startMessageIndex);
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

    public void stop() {
        tcpClient.pause();
    }

    public void closeConnection() {
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

    public void setErrorListener(OnErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public interface OnErrorListener {
        void onNetworkError(JSTPConnectionException e);
    }
}