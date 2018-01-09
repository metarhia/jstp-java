package com.metarhia.jstp.connection;

import com.metarhia.jstp.Constants;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSSerializer;
import com.metarhia.jstp.core.JSTypes.JSTypesUtil;
import com.metarhia.jstp.storage.StorageInterface;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Connection implements
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
  private static final String STREAM = "stream";
  private static final String INSPECT = "inspect";
  private static final String PING = "ping";
  private static final String PONG = "pong";

  private static final Map<String, Method> METHOD_HANDLERS = new HashMap<>(10);

  private static final Logger logger = LoggerFactory.getLogger(Connection.class);

  private static AtomicLong nextConnectionID = new AtomicLong(0);

  static {
    try {
      METHOD_HANDLERS.put(HANDSHAKE,
          Connection.class.getDeclaredMethod("handshakeMessageHandler", JSObject.class));
      METHOD_HANDLERS.put(CALL,
          Connection.class.getDeclaredMethod("callMessageHandler", JSObject.class));
      METHOD_HANDLERS.put(CALLBACK,
          Connection.class.getDeclaredMethod("callbackMessageHandler", JSObject.class));
      METHOD_HANDLERS.put(EVENT,
          Connection.class.getDeclaredMethod("eventMessageHandler", JSObject.class));
      METHOD_HANDLERS.put(INSPECT,
          Connection.class.getDeclaredMethod("inspectMessageHandler", JSObject.class));
      METHOD_HANDLERS.put(PING,
          Connection.class.getDeclaredMethod("pingMessageHandler", JSObject.class));
      METHOD_HANDLERS.put(PONG,
          Connection.class.getDeclaredMethod("pongMessageHandler", JSObject.class));
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Cannot create method handlers", e);
    }
  }

  private long id;

  private ConnectionState state;

  /**
   * Transport to send/receive messages
   */
  private AbstractSocket transport;

  /**
   * Call handlers table. Handlers are associated with names of methods they handle
   */
  private Map<String, Map<String, ManualHandler>> callHandlers;

  /**
   * Event handlers table. Handlers are associated with names of interfaces they handle
   */
  private Map<String, Map<String, List<ManualHandler>>> eventHandlers;

  /**
   * Callback, stream and handshake handlers. Handlers are associated with numbers of
   * packages incoming to server.
   */
  private Map<Long, ManualHandler> handlers;

  /**
   * Client method names for incoming inspect packages by interface
   */
  private Map<String, List<String>> clientMethodNames;

  private List<ConnectionListener> connectionListeners;

  private RestorationPolicy restorationPolicy;

  private SessionData sessionData;

  private Queue<Message> sendQueue;

  private int sendBufferCapacity;

  private NoConnBufferingPolicy noConnBufferingPolicy;

  public Connection(AbstractSocket transport) {
    this(transport, new SessionRestorationPolicy());
  }

  public Connection(AbstractSocket transport, RestorationPolicy restorationPolicy) {
    this.id = nextConnectionID.getAndIncrement();
    this.state = ConnectionState.STATE_AWAITING_HANDSHAKE;
    this.sessionData = new SessionData();
    this.sendBufferCapacity = DEFAULT_SEND_BUFFER_CAPACITY;
    this.sendQueue = new ConcurrentLinkedQueue<>();

    this.transport = transport;
    this.transport.setSocketListener(this);
    state = ConnectionState.STATE_AWAITING_HANDSHAKE;

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
      this.transport.setSocketListener(null);
      this.transport.close(true);
    }

    this.transport = transport;
    this.transport.setSocketListener(this);
    state = ConnectionState.STATE_AWAITING_HANDSHAKE;

    if (sessionData.getAppName() != null) {
      connect(sessionData.getAppName());
    }
  }

  private boolean restoreSession(long numServerReceivedMessages) {
    long redundantMessages =
        sendQueue.size() - (sessionData.getNumSentMessages() - numServerReceivedMessages);
    sessionData.setNumSentMessages(numServerReceivedMessages);
    while (redundantMessages-- > 0) {
      sendQueue.poll();
    }

    return restorationPolicy != null && restorationPolicy.restore(this, sendQueue);
  }

  /**
   * Calls {@link #connect(String, String)} with {@param appName}
   * and null for sessionID
   */
  public void connect(String appName) {
    connect(appName, null);
  }

  /**
   * Checks if transport is connected, if it is not calls {@link AbstractSocket#connect()}
   * else initiates a handshake
   *
   * @param appName   name of the application to use during handshake (must not be null)
   * @param sessionID optional id to restore session
   */
  public void connect(String appName, String sessionID) {
    if (appName == null) {
      throw new RuntimeException("Application name must not be null");
    }
    sessionData.setAppName(appName);
    if (sessionID != null) {
      sessionData.setSessionID(sessionID);
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
   *                the server
   */
  public void handshake(String appName, ManualHandler handler) {
    handshake(appName, null, null, handler);
  }

  /**
   * Tries to restores session with id {@param sessionID}
   *
   * @param appName   application name to denote application on server
   * @param sessionID id of a session to restore
   * @param handler   optional handler that will be called when response handshake message comes
   *                  from the server
   */
  public void handshake(String appName, String sessionID, ManualHandler handler) {
    if (appName == null) {
      throw new RuntimeException("Application name must not be null");
    }
    sessionData.setAppName(appName);
    sessionData.setSessionID(sessionID);
    long packageCounter = 0;
    if (handler != null) {
      handlers.put(packageCounter, handler);
    }

    Message hm;
    List<?> args = Arrays.asList(sessionID, sessionData.getNumReceivedMessages());
    hm = new Message(packageCounter, HANDSHAKE)
        .putArg("session", args);
    hm.addProtocolArg(appName);

    send(hm, false);
  }

  /**
   * Sends handshake message with authorization
   *
   * @param appName  application name to denote application on server
   * @param username login for authorization on server
   * @param password password for authorization on server
   * @param handler  optional handler that will be called when response handshake message comes from
   *                 the server
   */
  public void handshake(String appName, String username, String password, ManualHandler handler) {
    if (appName == null) {
      throw new RuntimeException("Application name must not be null");
    }
    sessionData.setAppName(appName);
    long packageCounter = 0;
    if (handler != null) {
      handlers.put(packageCounter, handler);
    }

    Message hm = new Message(packageCounter, HANDSHAKE)
        .addProtocolArg(appName);
    if (username != null && password != null) {
      hm.putArg(username, password);
    }

    send(hm, false);
  }

  public void call(String interfaceName,
                   String methodName,
                   List<?> args,
                   ManualHandler handler) {
    long messageNumber = sessionData.getAndIncrementMessageCounter();
    Message callMessage = new Message(messageNumber, CALL)
        .putArg(methodName, args)
        .addProtocolArg(interfaceName);

    if (handler != null) {
      handlers.put(messageNumber, handler);
    }

    send(callMessage);
  }

  public void callback(JSCallback result, List<?> args) {
    callback(result, args, null);
  }

  public void callback(JSCallback result, List<?> args, Long messageNumber) {
    if (messageNumber == null) {
      messageNumber = sessionData.getAndIncrementMessageCounter();
    }

    Message callbackMessage = new Message(messageNumber, CALLBACK)
        .putArg(result.toString(), args);

    send(callbackMessage);
  }

  public void inspect(String interfaceName, ManualHandler handler) {
    long messageNumber = sessionData.getAndIncrementMessageCounter();
    Message inspectMessage = new Message(messageNumber, INSPECT)
        .addProtocolArg(interfaceName);

    if (handler != null) {
      handlers.put(messageNumber, handler);
    }

    send(inspectMessage);
  }

  public void event(String interfaceName, String eventName, List<?> args) {
    long messageNumber = sessionData.getAndIncrementMessageCounter();
    Message eventMessage = new Message(messageNumber, EVENT)
        .putArg(eventName, args)
        .addProtocolArg(interfaceName);

    send(eventMessage);
  }

  private void send(Message message) {
    send(message, true);
  }

  private void send(Message message, boolean buffer) {
    if (transport.isConnected() || noConnBufferingPolicy == NoConnBufferingPolicy.BUFFER) {
      final String stringRepresentation =
          JSSerializer.stringify(message.getMessage()) + TERMINATOR;
      message.setStringRepresentation(stringRepresentation);
      if (buffer) {
        // policy can only be applied to messages that actually can be buffered
        sendQueue.offer(message);
        if (sendQueue.size() >= sendBufferCapacity) {
          sendQueue.poll();
        }
      }
    }

    if (transport.isConnected()) {
      // for now no buffering means no counting
      send(message.getStringRepresentation(), buffer);
    }
  }

  /**
   * Should usually only be called in {@link RestorationPolicy}
   *
   * Sends message directly to the transport and increases number
   * of sent messages if {@param count} is true
   *
   * @param message string to be passed to the transport
   * @param count   if true increases number of sent messages by one
   */
  public void send(String message, boolean count) {
    if (count) {
      sessionData.incrementNumSentMessages();
    }
    transport.send(message);
  }

  public void addSocketListener(ConnectionListener listener) {
    this.connectionListeners.add(listener);
  }

  @Override
  public void onMessageReceived(JSObject message) {
    try {
      boolean handshake = false;
      if (message.isEmpty()) {
        // heartbeat packet
        return;
      }
      if (!(handshake = message.getKey(0).equals(HANDSHAKE))
          && state != ConnectionState.STATE_CONNECTED) {
        rejectMessage(message);
      } else {
        final Method handler = METHOD_HANDLERS.get(message.getKey(0));
        if (handler != null) {
          handler.invoke(this, message);
        } else {
          rejectMessage(message);
        }
      }
      if (!handshake) {
        sessionData.incrementNumReceivedMessages();
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Cannot find or access message handler method", e);
    } catch (ClassCastException | NullPointerException | InvocationTargetException e) {
      // means message was ill formed
      rejectMessage(message);
    }
  }

  private void handshakeMessageHandler(JSObject<?> message) {
    if (state == ConnectionState.STATE_CONNECTED) {
      rejectMessage(message);
      return;
    }

    final String payloadKey = message.getKey(1);
    final Object payload = message.get(payloadKey);
    if (payloadKey.equals("error")) {
      int errorCode = JSTypesUtil.<Double>getMixed(payload, 0).intValue();
      reportError(errorCode);
      close(true);
    } else {
      state = ConnectionState.STATE_CONNECTED;
      boolean restored = false;
      if (payload instanceof Double) {
        restored = processHandshakeRestoreResponse(message);
        if (!restored) {
          reset(sessionData.getAppName(), 1);
        }
      } else if (payload instanceof String) {
        processHandshakeResponse(message);
      }
      reportConnected(restored);
    }
  }

  private boolean processHandshakeRestoreResponse(JSObject message) {
    if (sessionData.getMessageCounter().get() == 0) {
      sessionData.getAndIncrementMessageCounter();
    }
    long numServerReceivedMessages = ((Double) message.getByIndex(1)).longValue();
    boolean restored = restoreSession(numServerReceivedMessages);

    long receiverIndex = 0;
    ManualHandler callbackHandler = handlers.remove(receiverIndex);
    if (callbackHandler != null) {
      callbackHandler.handle(message);
    }

    return restored;
  }

  private void processHandshakeResponse(JSObject message) {
    sessionData.setSessionID((String) message.getByIndex(1));
    long receiverIndex = 0;
    ManualHandler callbackHandler = handlers.remove(receiverIndex);

    // always reset connection on basic handshake
    // count first handshake
    reset(sessionData.getAppName(), 1);

    if (callbackHandler != null) {
      callbackHandler.handle(message);
    }
  }

  private void callMessageHandler(JSObject message) {
    String interfaceName = getInterfaceName(message);
    if (!isSecondArray(message)) {
      rejectMessage(message);
      return;
    }
    String methodName = message.getKey(1);

    final Map<String, ManualHandler> iface = callHandlers.get(interfaceName);
    if (iface == null) {
      return;
    }

    ManualHandler handler = iface.get(methodName);
    if (handler != null) {
      handler.handle(message);
    }
  }

  private void callbackMessageHandler(JSObject message) {
    long receiverIndex = getMessageNumber(message);
    if (!isSecondArray(message)) {
      rejectMessage(message);
      return;
    }
    ManualHandler callbackHandler = handlers.remove(receiverIndex);
    if (callbackHandler != null) {
      callbackHandler.handle(message);
    }
  }

  private void eventMessageHandler(JSObject message) {
    String interfaceName = getInterfaceName(message);

    if (!isSecondArray(message)) {
      rejectMessage(message);
      return;
    }

    Map<String, List<ManualHandler>> interfaceHandlers = eventHandlers.get(interfaceName);
    if (interfaceHandlers == null) {
      return;
    }

    String eventName = message.getKey(1);
    List<ManualHandler> eventHandlers = interfaceHandlers.get(eventName);
    if (eventHandlers == null) {
      return;
    }

    for (ManualHandler eh : eventHandlers) {
      eh.handle(message);
    }
  }

  private void inspectMessageHandler(JSObject message) {
    long messageCounter = getMessageNumber(message);
    String interfaceName = getInterfaceName(message);
    List<String> methods = clientMethodNames.get(interfaceName);
    if (methods != null) {
      callback(JSCallback.OK, methods, messageCounter);
    } else {
      callback(JSCallback.ERROR,
          Collections.singletonList(Constants.ERR_INTERFACE_NOT_FOUND), messageCounter);
    }
  }

  private void pingMessageHandler(JSObject message) {
    long pingNumber = getMessageNumber(message);
    Message streamMessage = new Message(pingNumber, PONG);
    send(streamMessage);
  }

  private void pongMessageHandler(JSObject message) {
    callbackMessageHandler(message);
  }

  public void setCallHandler(String interfaceName, String methodName, ManualHandler callHandler) {
    Map<String, ManualHandler> interfaceHandlers = callHandlers.get(interfaceName);
    if (interfaceHandlers == null) {
      callHandlers.put(interfaceName, new HashMap<String, ManualHandler>());
      interfaceHandlers = callHandlers.get(interfaceName);
    }
    interfaceHandlers.put(methodName, callHandler);
  }

  public void removeCallHandler(String interfaceName, String methodName) {
    final Map<String, ManualHandler> interfaceHandlers = callHandlers.get(interfaceName);
    if (interfaceHandlers != null) {
      interfaceHandlers.remove(methodName);
    }
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

  public void addHandler(long messageNumber, ManualHandler manualHandler) {
    handlers.put(messageNumber, manualHandler);
  }

  private void reset() {
    reset(null, 0);
  }

  private void reset(String appName, long messageCounter) {
    sessionData = new SessionData(appName, messageCounter);
    handlers = new ConcurrentHashMap<>();
    sendQueue.clear();
  }

  public void close() {
    close(false);
  }

  public void close(boolean forced) {
    reset();
    state = ConnectionState.STATE_CLOSING;
    if (transport != null) {
      transport.close(forced);
    }
  }

  public void pause() {
    transport.pause();
  }

  public void resume() {
    transport.resume();
  }

  private String getInterfaceName(JSObject message) {
    return JSTypesUtil.getMixed(message, 0.0, 1);
  }

  private long getMessageNumber(JSObject message) {
    return JSTypesUtil.<Double>getMixed(message, 0.0, 0).longValue();
  }

  private boolean isSecondArray(JSObject message) {
    return message.getByIndex(1) instanceof List;
  }

  public void setClientMethodNames(String interfaceName, String... names) {
    setClientMethodNames(interfaceName, Arrays.asList(names));
  }

  public void setClientMethodNames(String interfaceName, List<String> names) {
    List<String> methods = clientMethodNames.get(interfaceName);
    if (methods == null) {
      methods = new ArrayList<>();
    } else {
      methods.clear();
    }
    methods.addAll(names);
    clientMethodNames.put(interfaceName, methods);
  }

  public boolean isConnected() {
    return state == ConnectionState.STATE_CONNECTED;
  }

  public boolean isClosed() {
    return state == ConnectionState.STATE_CLOSED;
  }

  public String getSessionID() {
    return sessionData.getSessionID();
  }

  SessionData getSessionData() {
    return sessionData;
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
    if (sessionData.getAppName() == null
        || state != ConnectionState.STATE_AWAITING_RECONNECT
        && state != ConnectionState.STATE_AWAITING_HANDSHAKE) {
      return;
    }
    if (restorationPolicy != null) {
      restorationPolicy.onTransportAvailable(this,
          sessionData.getAppName(), sessionData.getSessionID());
    } else {
      handshake(sessionData.getAppName(), null);
      sendQueue.clear();
    }
    // todo add calls and fields for username\password auth
  }

  @Override
  public void onConnectionClosed() {
    if (state == ConnectionState.STATE_CLOSING) {
      state = ConnectionState.STATE_CLOSED;
      reportClosed();
    } else if (sessionData.getAppName() != null) {
      state = ConnectionState.STATE_AWAITING_RECONNECT;
      reportClosed();
    } else {
      state = ConnectionState.STATE_AWAITING_HANDSHAKE;
    }
  }

  @Override
  public void onError(Exception e) {
    // TODO: change signature of connectionError and pass it there
    logger.info("Transport error", e);
  }

  public void saveSession(StorageInterface storageInterface) {
    storageInterface.putSerializable(Constants.KEY_SESSION_DATA, sessionData);
  }

  public void restoreSession(StorageInterface storageInterface) {
    sessionData = (SessionData) storageInterface
        .getSerializable(Constants.KEY_SESSION_DATA, sessionData);
  }

  private void reportClosed() {
    for (ConnectionListener listener : connectionListeners) {
      listener.onConnectionClosed();
    }
  }

  private void reportConnected(boolean restored) {
    for (ConnectionListener listener : connectionListeners) {
      listener.onConnected(restored);
    }
  }

  private void reportError(int errorCode) {
    for (ConnectionListener listener : connectionListeners) {
      listener.onConnectionError(errorCode);
    }
  }

  private void rejectMessage(JSObject message) {
    close();
    for (ConnectionListener listener : connectionListeners) {
      listener.onMessageRejected(message);
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

  public ConnectionState getState() {
    return state;
  }

  public RestorationPolicy getRestorationPolicy() {
    return restorationPolicy;
  }

  public void setRestorationPolicy(RestorationPolicy restorationPolicy) {
    this.restorationPolicy = restorationPolicy;
  }
}
