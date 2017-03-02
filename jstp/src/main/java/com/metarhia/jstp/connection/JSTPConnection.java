package com.metarhia.jstp.connection;

import com.metarhia.jstp.Constants;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSTypes.JSArray;
import com.metarhia.jstp.core.JSTypes.JSNumber;
import com.metarhia.jstp.core.JSTypes.JSObject;
import com.metarhia.jstp.core.JSTypes.JSString;
import com.metarhia.jstp.core.JSTypes.JSTypesUtil;
import com.metarhia.jstp.core.JSTypes.JSValue;
import com.metarhia.jstp.handlers.StateHandler;
import com.metarhia.jstp.transport.TCPTransport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSTPConnection implements
    AbstractSocket.AbstractSocketListener {

  /**
   * Package terminator
   */
  public static final String TERMINATOR = "\0";

  private static final int DEFAULT_SEND_BUFFER_CAPACITY = 20;

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

  private static final Logger logger = LoggerFactory.getLogger(JSTPConnection.class);

  private static AtomicLong nextConnectionID = new AtomicLong(0);

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
//            METHOD_HANDLERS.put(STREAM,
//                    JSTPConnection.class.getDeclaredMethod("streamPacketHandler", JSObject.class));
      METHOD_HANDLERS.put(PING,
          JSTPConnection.class.getDeclaredMethod("pingPacketHandler", JSObject.class));
      METHOD_HANDLERS.put(PONG,
          JSTPConnection.class.getDeclaredMethod("pongPacketHandler", JSObject.class));
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
  }

  private long id;

  /**
   * Package counter
   */
  private int packageCounter;

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
   * Client method names for incoming inspect packages by interface
   */
  private Map<String, JSArray> clientMethodNames;

  private List<JSTPConnectionListener> connectionListeners;

  private RestorationPolicy restorationPolicy;

  private long numSentPackets;

  private long numReceivedPackets;

  private Queue<JSTPMessage> sendQueue;

  private int sendBufferCapacity;

  private NoConnBufferingPolicy noConnBufferingPolicy;

  private boolean handshakeFinished;

  private String sessionID;

  private String appName;

  @Deprecated
  public JSTPConnection(String host, int port) {
    this(host, port, true);
  }

  /**
   * Creates new JSTP connection
   *
   * @param host of the server
   * @param port of the server
   * @param sslEnabled determines whether connection will use SSL or not
   */
  @Deprecated
  public JSTPConnection(String host, int port, boolean sslEnabled) {
    this(new TCPTransport(host, port, sslEnabled));
  }

  public JSTPConnection(AbstractSocket transport) {
    this(transport, null);
  }

  public JSTPConnection(AbstractSocket transport, RestorationPolicy restorationPolicy) {
    this.id = nextConnectionID.getAndIncrement();
    this.sendBufferCapacity = DEFAULT_SEND_BUFFER_CAPACITY;
    this.sendQueue = new ConcurrentLinkedQueue<>();

    useTransport(transport);

    if (restorationPolicy == null) {
      restorationPolicy = new SessionRestorationPolicy(this);
    }
    this.restorationPolicy = restorationPolicy;

    this.connectionListeners = new ArrayList<>();
    this.eventHandlers = new ConcurrentHashMap<>();
    this.clientMethodNames = new ConcurrentHashMap<>();
    this.handlers = new ConcurrentHashMap<>();
    this.callHandlers = new ConcurrentHashMap<>();

    this.noConnBufferingPolicy = NoConnBufferingPolicy.BUFFER;
  }

  public void useTransport(AbstractSocket transport) {
    if (this.transport != null) {
      this.transport.close(true);
      this.transport.setSocketListener(null);
    }
    this.transport = transport;
    this.transport.setSocketListener(this);
  }

  @Deprecated
  public void createNewConnection(String host, int port, boolean sslEnabled) {
    useTransport(new TCPTransport(host, port, sslEnabled, this));
  }

  private boolean restoreSession(long numServerReceivedPackets) {
    long redundantPackets = sendQueue.size() - numSentPackets - numServerReceivedPackets;
    numSentPackets = numServerReceivedPackets;
    while (redundantPackets-- > 0) {
      sendQueue.poll();
    }

    return restorationPolicy != null && restorationPolicy.restore(sendQueue);
  }

  public void connect() {
    connect(null);
  }

  public void connect(String appName) {
    connect(appName, null);
  }

  public void connect(String appName, String sessionID) {
    if (appName != null) {
      this.appName = appName;
    }
    if (sessionID != null) {
      this.sessionID = sessionID;
    }
    if (!transport.isConnected()) {
      transport.connect();
    } else {
      onConnected();
    }
  }

  /**
   * Sends anonymous handshake message
   *
   * @param appName application name to denote application on server
   * @param handler optional handler that will be called when response handshake message comes from
   * the server
   */
  public void handshake(String appName, ManualHandler handler) {
    handshake(appName, null, null, handler);
  }

  /**
   * Tries to restores session with id {@param sessionID}
   *
   * @param appName application name to denote application on server
   * @param sessionID id of a session to restore
   * @param handler optional handler that will be called when response handshake message comes from
   * the server
   */
  public void handshake(String appName, String sessionID, ManualHandler handler) {
    this.appName = appName;
    this.sessionID = sessionID;
    int packageCounter = this.packageCounter++;
    if (handler != null) {
      handlers.put(packageCounter, handler);
    }

    JSTPMessage hm;
    JSArray args = new JSArray(new Object[]{sessionID, numReceivedPackets});
    hm = new JSTPMessage(packageCounter, HANDSHAKE, "session", args);
    hm.addProtocolArg(appName);

    send(hm, false);
  }

  /**
   * Sends handshake message with authorization
   *
   * @param appName application name to denote application on server
   * @param username login for authorization on server
   * @param password password for authorization on server
   * @param handler optional handler that will be called when response handshake message comes from
   * the server
   */
  public void handshake(String appName, String username, String password, ManualHandler handler) {
    this.appName = appName;
    int packageCounter = this.packageCounter++;
    if (handler != null) {
      handlers.put(packageCounter, handler);
    }

    JSTPMessage hm;
    if (username == null) {
      hm = new JSTPMessage(packageCounter, HANDSHAKE);
    } else {
      hm = new JSTPMessage(packageCounter, HANDSHAKE, username, new JSString(password));
    }
    hm.addProtocolArg(appName);

    send(hm, false);
  }

  public void call(String interfaceName,
      String methodName,
      JSArray args,
      ManualHandler handler) {
    int packageCounter = this.packageCounter++;
    JSTPMessage callMessage = new JSTPMessage(packageCounter, CALL, methodName, args);
    callMessage.addProtocolArg(interfaceName);

    if (handler != null) {
      handlers.put(packageCounter, handler);
    }

    send(callMessage);
  }

  public void callback(JSCallback value, JSValue args) {
    callback(value, args, null);
  }

  public void callback(JSCallback value, JSValue args, Integer customPackageIndex) {
    int packageNumber = customPackageIndex == null ? this.packageCounter++ : customPackageIndex;

    JSTPMessage callbackMessage = new JSTPMessage(packageNumber, CALLBACK, value.toString(), args);

    send(callbackMessage);
  }

  public void inspect(String interfaceName, ManualHandler handler) {
    int packageCounter = this.packageCounter++;
    JSTPMessage inspectMessage = new JSTPMessage(packageCounter, INSPECT);
    inspectMessage.addProtocolArg(interfaceName);

    if (handler != null) {
      handlers.put(packageCounter, handler);
    }

    send(inspectMessage);
  }

  public void event(String interfaceName, String methodName, JSArray args) {
    int packageCounter = this.packageCounter++;
    JSTPMessage eventMessage = new JSTPMessage(packageCounter, EVENT, methodName, args);
    eventMessage.addProtocolArg(interfaceName);

    send(eventMessage);
  }

  private void send(JSTPMessage message) {
    send(message, true);
  }

  private void send(JSTPMessage message, boolean buffer) {
    if (transport.isConnected() || noConnBufferingPolicy == NoConnBufferingPolicy.BUFFER) {
      message.setStringRepresentation(message.getMessage() + TERMINATOR);
      if (buffer) {
        // policy can only be applied to messages that actually can be buffered
        sendQueue.offer(message);
        if (sendQueue.size() >= sendBufferCapacity) {
          sendQueue.poll();
        }
      }
    }

    // for now no buffering means no counting
    if (transport.isConnected()) {
      send(message.getStringRepresentation(), buffer);
    }
  }

  public void send(String message) {
    send(message, true);
  }

  private void send(String message, boolean count) {
    if (count) {
      numSentPackets++;
    }
    transport.send(message);
  }

  public void addSocketListener(JSTPConnectionListener listener) {
    this.connectionListeners.add(listener);
  }

  @Override
  public void onPacketReceived(JSObject packet) {
    try {
      List<String> keys = packet.getOrderedKeys();
      boolean handshake = false;
      if (keys.size() == 0 || !(handshake = keys.get(0).equals(HANDSHAKE)) && !handshakeFinished) {
        rejectPacket(packet);
      } else {
        final Method handler = METHOD_HANDLERS.get(keys.get(0));
        if (handler != null) {
          handler.invoke(this, packet);
        } else {
          rejectPacket(packet);
        }
      }
      if (!handshake) {
        numReceivedPackets++;
      }
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  private void handshakePacketHandler(JSObject packet) {
    if (handshakeFinished) {
      rejectPacket(packet);
      return;
    }
    if (packet.getOrderedKeys().get(1).equals("error")) {
      sessionID = null;
      int errorCode = (int) JSTypesUtil.jsToJava(((JSArray) packet.get(1)).get(0), true);
      reportError(errorCode);
      close(true);
    } else {
      boolean restored = false;
      if (packet.get(1) instanceof JSString) {
        processHandshakeResponse(packet);
      } else if (packet.get(1) instanceof JSArray) {
        restored = processHandshakeRestoreResponse(packet);
      }
      if (!restored) {
        reset();
        packageCounter = 1;
      }
      reportConnected(restored);
    }
  }

  private boolean processHandshakeRestoreResponse(JSObject packet) {
    long numServerReceivedPackets = (long) JSTypesUtil.jsToJava(packet.get(1));
    return restoreSession(numServerReceivedPackets);
  }

  private void processHandshakeResponse(JSObject packet) {
    handshakeFinished = true;
    sessionID = (String) JSTypesUtil.jsToJava(packet.get(1));
    int receiverIndex = getPacketNumber(packet);
    ManualHandler callbackHandler = handlers.remove(receiverIndex);
    if (callbackHandler != null) {
      callbackHandler.invoke(packet);
    }
  }

  private void callPacketHandler(JSObject packet) {
    String methodName = packet.getOrderedKeys().get(1);
    ManualHandler handler = callHandlers.get(methodName);
    if (handler != null) {
      handler.invoke(packet);
    }
  }

  private void callbackPacketHandler(JSObject packet) {
    int receiverIndex = getPacketNumber(packet);
    ManualHandler callbackHandler = handlers.remove(receiverIndex);
    if (callbackHandler != null) {
      callbackHandler.invoke(packet);
    }
  }

  private void eventPacketHandler(JSObject packet) {
    String interfaceName = getInterfaceName(packet);
    Map<String, List<ManualHandler>> interfaceHandlers = eventHandlers.get(interfaceName);
    if (interfaceHandlers == null) {
      return;
    }

    String eventName = getEventName(packet);
    List<ManualHandler> eventHandlers = interfaceHandlers.get(eventName);
    if (eventHandlers == null) {
      return;
    }

    for (ManualHandler eh : eventHandlers) {
      eh.invoke(packet);
    }
  }

  private void inspectPacketHandler(JSObject packet) {
    final JSValue args = packet.get(0);
    if (!(args instanceof JSArray)
        || ((JSArray) args).size() < 2
        || !(((JSArray) args).get(1) instanceof JSString)) {
      rejectPacket(packet);
      return;
    }

    String interfaceName = ((JSString) ((JSArray) args).get(1)).getValue();
    JSArray methods = clientMethodNames.get(interfaceName);
    if (methods != null) {
      callback(JSCallback.OK, methods);
    } else {
      callback(JSCallback.ERROR, new JSNumber(Constants.ERR_INTERFACE_NOT_FOUND));
    }
  }

  private void statePacketHandler(JSObject packet) {
    stateHandler.onState(packet);
  }

  private void pingPacketHandler(JSObject packet) {
    int pingNumber = getPacketNumber(packet);
    JSTPMessage streamMessage = new JSTPMessage(pingNumber, PONG);
    send(streamMessage);
  }

  private void pongPacketHandler(JSObject packet) {
    callbackPacketHandler(packet);
  }

//    private void streamPacketHandler(JSObject packet) {
//        int receiverIndex = getPacketNumber(packet);
//        ManualHandler responseHandler = handlers.get(receiverIndex);
//        if (responseHandler != null) responseHandler.invoke(packet);
//    }

//    public int openStream(JSValue data) {
//        int packageCounter = nextPackageCounter();
//        JSTPMessage streamMessage = new JSTPMessage(packageCounter, STREAM, STREAM_DATA, data);
//        transport.send(streamMessage.getMessage() + TERMINATOR);
//        return packageCounter;
//    }
//
//    public void writeStream(int packageNumber, JSValue data) {
//        JSTPMessage streamMessage = new JSTPMessage(packageNumber, STREAM, STREAM_DATA, data);
//        transport.send(streamMessage.getMessage() + TERMINATOR);
//    }

  @Deprecated
  public void addCallHandler(String methodName, ManualHandler callHandler) {
    setCallHandler(methodName, callHandler);
  }

  public void setCallHandler(String methodName, ManualHandler callHandler) {
    callHandlers.put(methodName, callHandler);
  }

  public void removeCallHandler(String methodName) {
    callHandlers.remove(methodName);
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
      if (eventHandlers != null) {
        eventHandlers.remove(handler);
      }
    }
  }

  public void addHandler(int packetNumber, ManualHandler manualHandler) {
    handlers.put(packetNumber, manualHandler);
  }

  private void reset() {
    packageCounter = 0;
    numReceivedPackets = 0;
    numSentPackets = 0;
    handlers = new ConcurrentHashMap<>();
    callHandlers = new ConcurrentHashMap<>();
  }

  public void close() {
    close(false);
  }

  public void close(boolean forced) {
    if (transport != null) {
      transport.close(forced);
    }
    sessionID = null;
    reset();
  }

  public void pause() {
    transport.pause();
  }

  public void resume() {
    transport.resume();
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

  public void setClientMethodNames(String interfaceName, String... names) {
    JSArray methods = clientMethodNames.get(interfaceName);
    if (methods == null) {
      methods = new JSArray();
    } else {
      methods.clear();
    }
    for (String name : names) {
      methods.add(name);
    }
    clientMethodNames.put(interfaceName, methods);
  }

  public boolean isConnected() {
    return handshakeFinished && transport.isConnected();
  }

  public boolean isClosed() {
    return transport.isClosed();
  }

  public String getSessionID() {
    return sessionID;
  }

  public boolean isHandshakeFinished() {
    return handshakeFinished;
  }

  void setHandshakeFinished(boolean handshakeFinished) {
    this.handshakeFinished = handshakeFinished;
  }

  public long getId() {
    return id;
  }

  public void clearQueue() {
    sendQueue.clear();
    transport.clearQueue();
  }

  @Override
  public void onConnected() {
    if (appName == null) {
      return;
    }
    if (restorationPolicy != null) {
      restorationPolicy.onTransportAvailable(appName, sessionID);
    } else {
      handshake(appName, null);
    }
    // todo add calls and fields for username\password auth
  }

  @Override
  public void onConnectionClosed(int remainingMessages) {
    handshakeFinished = false;
    numSentPackets -= remainingMessages;
    for (JSTPConnectionListener listener : connectionListeners) {
      listener.onConnectionClosed();
    }
  }

  @Override
  public void onError(Exception e) {
    logger.info("Transport error", e);
  }

  private void reportConnected(boolean restored) {
    for (JSTPConnectionListener listener : connectionListeners) {
      listener.onConnected(restored);
    }
  }

  private void reportError(int errorCode) {
    for (JSTPConnectionListener listener : connectionListeners) {
      listener.onConnectionError(errorCode);
    }
  }

  private void rejectPacket(JSObject packet) {
    for (JSTPConnectionListener listener : connectionListeners) {
      listener.onPacketRejected(packet);
    }
  }

  public AbstractSocket getTransport() {
    return transport;
  }

  public int getSendBufferCapacity() {
    return sendBufferCapacity;
  }

  public void setSendBufferCapacity(int sendBufferCapacity) {
    this.sendBufferCapacity = sendBufferCapacity;
  }
}