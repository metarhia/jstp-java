package com.metarhia.jstp.Connection;

import com.metarhia.jstp.Handlers.StateHandler;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSParsingException;
import com.metarhia.jstp.core.JSTypes.JSNumber;
import com.metarhia.jstp.JSTP;
import com.metarhia.jstp.core.JSTypes.JSArray;
import com.metarhia.jstp.core.JSTypes.JSObject;
import com.metarhia.jstp.core.JSTypes.JSValue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Lida on 27.06.16.
 */
public class JSTPConnection extends TCPClient.TCPMessageReceiver {

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
    private static final String TERMINATOR = ",{\f},";
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

    public JSTPConnection(String host, int port) {
        this.tcpClient = new TCPClient(host, port);
        handlers = new HashMap<>();
        eventHandlers = new HashMap<>();
        callHandlers = new HashMap<>();
        messageBuilder = new StringBuilder();
        tcpClient.setMessageReceiver(this);
        clientMethodNames = new JSArray();
    }

    public void handshake(String applicationName, ManualHandler... handler) {
        if (handler.length != 0) {
            handlers.put(packageCounter, handler[0]);
        }

        JSTPMessage hm = new JSTPMessage(packageCounter, HANDSHAKE);
        hm.addProtocolArg(applicationName);

        tcpClient.openConnection(hm.getMessage(), TERMINATOR);
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
    protected void onMessageReceived(String received) {
        messageBuilder.append(received);

        int startMessageIndex = 0;
        int endMessageIndex = messageBuilder.indexOf(TERMINATOR);
        while (endMessageIndex != -1) {
            String msg = messageBuilder.substring(startMessageIndex, endMessageIndex);
            int receiverIndex;

            try {
                JSObject messageObject = JSTP.parse(msg);
                List<String> keys = messageObject.getOrderedKeys();
                switch (keys.get(0)) {
                    case CALLBACK:
                    case HANDSHAKE:
                    case STREAM:
                        receiverIndex = getReceiverIndex(messageObject, keys.get(0));
                        ManualHandler responseHandler = handlers.get(receiverIndex);
                        if (responseHandler != null) {
                            responseHandler.invoke(messageObject);
                        }
                        break;
                    case CALL:
                        for (Map.Entry<String, ManualHandler> me : callHandlers.entrySet()) {
                            if (messageObject.containsKey(me.getKey())) {
                                me.getValue().invoke(messageObject);
                                packageCounter++;
                                break;
                            }
                        }
                        break;
                    case EVENT:
                        for (String interfaceName : eventHandlers.keySet()) {
                            if (messageObject.containsKey(interfaceName)) {
                                for (ManualHandler eh : eventHandlers.get(interfaceName)) {
                                    eh.invoke(messageObject);
                                }
                                packageCounter++;
                                break;
                            }
                        }
                        break;
                    case STATE:
                        stateHandler.onState(messageObject);
                        break;
                    case INSPECT:
                        handleInspect();
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
        packageCounter++;
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
            ehs = eventHandlers.put(eventInterfaceName, new LinkedList<ManualHandler>());
        }
        ehs.add(handler);
    }

    public void removeEventHandler(String eventInterfaceName, ManualHandler handler) {
        List<ManualHandler> ehs = eventHandlers.get(eventInterfaceName);
        ehs.remove(handler);
    }

    public void stop() {
        tcpClient.stop();
    }

    public void closeConnection() {
        tcpClient.close();
    }

    private int getReceiverIndex(JSObject messageObject, String packageValue) {
        return (int) ((JSNumber) ((JSArray) messageObject.get(packageValue))
                .get(0))
                .getValue();
    }

//    private static class Handler {
//        public Class clazz;
//        public Object receiver;
//        public List<String> fields;
//
//        public Handler(Class clazz, Object receiver) {
//            this(clazz, receiver, null);
//        }
//
//        public Handler(Class clazz, Object receiver, List<String> fields) {
//            this.receiver = receiver;
//            this.clazz = clazz;
//            this.fields = fields;
//        }
//
//        public void invoke(JSValue message) {
//            List<JSValue> fieldArgs = new LinkedList<>();
//
//            JSObject msg = (JSObject) message;
//
//            if (fields == null) {
//                Iterator<Map.Entry<String, JSValue>> iterator = msg.entrySet().iterator();
//                if (iterator.hasNext()) {
//                    JSValue args = ((JSArray) iterator.next().getValue()).get(1);
//                    fieldArgs.add(args);
//                }
//            } else {
//                for (String f : fields) {
//                    JSValue args = msg.get(f);
//                    fieldArgs.add(args);
//                }
//            }
//
//            for (Method m : clazz.getDeclaredMethods()) {
//                Object[] args = matchArgumentsToTypes(fieldArgs, m.getParameterTypes());
//                try {
//                    m.invoke(receiver, args);
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                } catch (InvocationTargetException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
}