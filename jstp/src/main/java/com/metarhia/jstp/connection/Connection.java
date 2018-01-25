package com.metarhia.jstp.connection;

import com.metarhia.jstp.Constants;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSTypes.JSTypesUtil;
import com.metarhia.jstp.exceptions.AlreadyConnectedException;
import com.metarhia.jstp.exceptions.ConnectionException;
import com.metarhia.jstp.storage.StorageInterface;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

  private static final Map<MessageType, Method> METHOD_HANDLERS = new HashMap<>(10);

  private static final Logger logger = LoggerFactory.getLogger(Connection.class);

  private static AtomicLong nextConnectionID = new AtomicLong(0);

  static {
    try {
      METHOD_HANDLERS.put(MessageType.HANDSHAKE,
          Connection.class.getDeclaredMethod("handshakeMessageHandler", JSObject.class));
      METHOD_HANDLERS.put(MessageType.CALL,
          Connection.class.getDeclaredMethod("callMessageHandler", JSObject.class));
      METHOD_HANDLERS.put(MessageType.CALLBACK,
          Connection.class.getDeclaredMethod("callbackMessageHandler", JSObject.class));
      METHOD_HANDLERS.put(MessageType.EVENT,
          Connection.class.getDeclaredMethod("eventMessageHandler", JSObject.class));
      METHOD_HANDLERS.put(MessageType.INSPECT,
          Connection.class.getDeclaredMethod("inspectMessageHandler", JSObject.class));
      METHOD_HANDLERS.put(MessageType.PING,
          Connection.class.getDeclaredMethod("pingMessageHandler", JSObject.class));
      METHOD_HANDLERS.put(MessageType.PONG,
          Connection.class.getDeclaredMethod("pongMessageHandler", JSObject.class));
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Cannot create method handlers", e);
    }
  }

  private long id;

  private ConnectionState state;

  private final Object stateLock;

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

  private SessionPolicy sessionPolicy;

  private AtomicLong messageNumberCounter;

  public Connection(AbstractSocket transport) {
    this(transport, new SimpleSessionPolicy());
  }

  public Connection(AbstractSocket transport, SessionPolicy sessionPolicy) {
    this.id = nextConnectionID.getAndIncrement();
    this.state = ConnectionState.AWAITING_HANDSHAKE;
    this.stateLock = new Object();
    this.messageNumberCounter = new AtomicLong(0);

    this.transport = transport;
    this.transport.setSocketListener(this);

    this.sessionPolicy = sessionPolicy;

    this.connectionListeners = new ArrayList<>();
    this.eventHandlers = new ConcurrentHashMap<>();
    this.clientMethodNames = new ConcurrentHashMap<>();
    this.handlers = new ConcurrentHashMap<>();
    this.callHandlers = new ConcurrentHashMap<>();
  }

  public void useTransport(AbstractSocket transport) {
    synchronized (stateLock) {
      if (this.transport != null) {
        this.transport.setSocketListener(null);
        this.transport.close(true);
      }

      this.transport = transport;
      this.transport.setSocketListener(this);
      this.state = ConnectionState.AWAITING_HANDSHAKE;
    }

    String appName = getAppName();
    if (appName != null) {
      connect(appName);
    }
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
    sessionPolicy.getSessionData().setParameters(appName, sessionID);
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
    sessionPolicy.getSessionData().setParameters(appName, sessionID);
    setMessageNumberCounter(sessionPolicy.getSessionData().getNumSentMessages());
    long messageNumber = 0;
    if (handler != null) {
      handlers.put(messageNumber, handler);
    }

    Message hm = new Message(messageNumber, MessageType.HANDSHAKE)
        .putArgs("session", sessionID,
            sessionPolicy.getSessionData().getNumReceivedMessages());
    hm.addProtocolArg(appName);

    sendHandshake(hm.stringify());
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
    sessionPolicy.getSessionData().setAppName(appName);
    setMessageNumberCounter(0);
    long messageNumber = getNextMessageNumber();
    if (handler != null) {
      handlers.put(messageNumber, handler);
    }

    Message hm = new Message(messageNumber, MessageType.HANDSHAKE)
        .addProtocolArg(appName);
    if (username != null && password != null) {
      hm.putArg(username, password);
    }
    sendHandshake(hm.stringify());
  }

  private void sendHandshake(String handshake) {
    synchronized (stateLock) {
      if (transport.isConnected() &&
          (state == ConnectionState.AWAITING_HANDSHAKE
              || state == ConnectionState.AWAITING_RECONNECT)) {
        state = ConnectionState.AWAITING_HANDSHAKE_RESPONSE;
        transport.send(handshake + TERMINATOR);
      }
    }
  }

  public void call(String interfaceName,
                   String methodName,
                   List<?> args,
                   ManualHandler handler) {
    long messageNumber = getNextMessageNumber();
    Message callMessage = new Message(messageNumber, MessageType.CALL)
        .putArg(methodName, args)
        .addProtocolArg(interfaceName);

    if (handler != null) {
      handlers.put(messageNumber, handler);
    }

    sendBuffered(callMessage);
  }

  public void callback(JSCallback result, List<?> args) {
    callback(result, args, null);
  }

  public void callback(JSCallback result, List<?> args, Long messageNumber) {
    if (messageNumber == null) {
      messageNumber = getNextMessageNumber();
    }

    Message callbackMessage = new Message(messageNumber, MessageType.CALLBACK)
        .putArg(result.toString(), args);

    send(callbackMessage);
  }

  public void inspect(String interfaceName, ManualHandler handler) {
    long messageNumber = getNextMessageNumber();
    Message inspectMessage = new Message(messageNumber, MessageType.INSPECT)
        .addProtocolArg(interfaceName);

    if (handler != null) {
      handlers.put(messageNumber, handler);
    }

    sendBuffered(inspectMessage);
  }

  public void event(String interfaceName, String eventName, List<?> args) {
    long messageNumber = getNextMessageNumber();
    Message eventMessage = new Message(messageNumber, MessageType.EVENT)
        .putArg(eventName, args)
        .addProtocolArg(interfaceName);

    sendBuffered(eventMessage);
  }

  /**
   * Should usually only be called in {@link SessionPolicy}
   *
   * Sends message that should be buffered if applicable
   *
   * @param message string to be passed to the transport
   */
  public void sendBuffered(Message message) {
    sessionPolicy.onMessageSent(message);
    send(message.stringify());
  }

  public void send(Message message) {
    send(message.stringify());
  }

  /**
   * Sends message directly to the transport if it's connected
   *
   * @param message string to be passed to the transport
   */
  public void send(String message) {
    if (transport.isConnected()) {
      transport.send(message + TERMINATOR);
    }
  }

  @Override
  public void onMessageReceived(JSObject message) {
    try {
      if (message.isEmpty()) {
        // heartbeat packet
        return;
      }
      MessageType messageType = MessageType.fromString(message.getKey(0));
      final Method handler = METHOD_HANDLERS.get(messageType);
      if (messageType != MessageType.HANDSHAKE && state != ConnectionState.CONNECTED
          || handler == null) {
        rejectMessage(message);
      } else {
        handler.invoke(this, message);
        sessionPolicy.onMessageReceived(message, messageType);
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Cannot find or access message handler method", e);
    } catch (ClassCastException | NullPointerException | InvocationTargetException e) {
      // means message was ill formed
      rejectMessage(message);
    }
  }

  private void handshakeMessageHandler(JSObject message) {
    synchronized (stateLock) {
      if (state != ConnectionState.AWAITING_HANDSHAKE_RESPONSE) {
        // if transport is not connected it means that the connection was closed before
        // this method was called so don't report this
        if (transport.isConnected()) {
          rejectMessage(message);
        }
        return;
      }

      final String payloadKey = message.getKey(1);
      final Object payload = message.get(payloadKey);
      if (payloadKey.equals("error")) {
        int errorCode = JSTypesUtil.<Double>getMixed(payload, 0).intValue();
        reportError(errorCode);
        close(true);
      } else if (transport.isConnected()) {
        // make sure transport is still connected to avoid extra work
        state = ConnectionState.CONNECTED;
        boolean restored = false;
        if (payload instanceof Double) {
          restored = processHandshakeRestoreResponse(message);
          if (restored) {
            setMessageNumberCounter(sessionPolicy.getSessionData().getNumSentMessages());
          } else {
            reset();
          }
          // count first handshake or last message
          getNextMessageNumber();
        } else if (payload instanceof String) {
          processHandshakeResponse(message);
        } else {
          rejectMessage(message);
          return;
        }
        reportConnected(restored);
      }
    }
  }

  private boolean processHandshakeRestoreResponse(JSObject message) {
    if (getMessageNumberCounter() == 0) {
      getNextMessageNumber();
    }
    long numServerReceivedMessages = ((Double) message.getByIndex(1)).longValue();
    boolean restored = sessionPolicy.restore(this, numServerReceivedMessages);

    long receiverIndex = 0;
    ManualHandler callbackHandler = handlers.remove(receiverIndex);
    if (callbackHandler != null) {
      callbackHandler.handle(message);
    }

    return restored;
  }

  private void processHandshakeResponse(JSObject message) {
    sessionPolicy.getSessionData().setSessionId((String) message.getByIndex(1));
    long receiverIndex = 0;
    ManualHandler handshakeHandler = handlers.remove(receiverIndex);

    // always reset connection on basic handshake
    reset();
    // count first handshake
    getNextMessageNumber();

    if (handshakeHandler != null) {
      handshakeHandler.handle(message);
    }
  }

  private void callMessageHandler(JSObject message) {
    String interfaceName = getInterfaceName(message);
    if (isSecondNotArray(message)) {
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
    if (isSecondNotArray(message)) {
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

    if (isSecondNotArray(message)) {
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
    Message pongMessage = new Message(pingNumber, MessageType.PONG);
    sendBuffered(pongMessage);
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

  public void saveSession(StorageInterface storageInterface) {
    sessionPolicy.saveSession(storageInterface);
  }

  public SessionData getSessionData() {
    return sessionPolicy.getSessionData();
  }

  public void saveSession(StorageInterface storageInterface, boolean storeBuffer) {
    sessionPolicy.saveSession(storageInterface);
  }

  public void restoreSession(StorageInterface storageInterface) {
    sessionPolicy.restoreSession(storageInterface);
  }

  private long getNextMessageNumber() {
    return messageNumberCounter.getAndIncrement();
  }

  private long setMessageNumberCounter(long messageNumber) {
    messageNumberCounter.set(messageNumber);
    return messageNumber;
  }

  private void reset() {
    reset(null, 0);
  }

  private void reset(String appName, long messageCounter) {
    setMessageNumberCounter(messageCounter);
    handlers = new ConcurrentHashMap<>();
    sessionPolicy.reset(appName);
  }

  public void close() {
    close(false);
  }

  public void close(boolean forced) {
    synchronized (stateLock) {
      if (state == ConnectionState.CLOSING) {
        return;
      }
      state = ConnectionState.CLOSING;
      reset();
      if (transport != null) {
        transport.close(forced);
      }
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

  public static long getMessageNumber(JSObject message) {
    return JSTypesUtil.<Double>getMixed(message, 0.0, 0).longValue();
  }

  private static boolean isSecondNotArray(JSObject message) {
    return !(message.getByIndex(1) instanceof List);
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
    return state == ConnectionState.CONNECTED;
  }

  public boolean isClosed() {
    return state == ConnectionState.CLOSED;
  }

  public long getId() {
    return id;
  }

  @Override
  public void onConnected() {
    synchronized (stateLock) {
      if (getAppName() == null
          || state != ConnectionState.AWAITING_RECONNECT
          && state != ConnectionState.AWAITING_HANDSHAKE) {
        return;
      }
      sessionPolicy.onTransportAvailable(this);
    }
    // todo add calls and fields for username\password auth
  }

  @Override
  public void onConnectionClosed() {
    synchronized (stateLock) {
      if (state == ConnectionState.CLOSING) {
        state = ConnectionState.CLOSED;
        reportClosed();
      } else if (getAppName() != null) {
        state = ConnectionState.AWAITING_RECONNECT;
        reportClosed();
      } else {
        state = ConnectionState.AWAITING_HANDSHAKE;
      }
      transport.clearQueue();
    }
  }

  @Override
  public void onError(Exception e) {
    logger.info("Transport error", e);
    if (!(e instanceof AlreadyConnectedException) && transport.isConnected()) {
      transport.close(true);
    }
  }

  public void addSocketListener(ConnectionListener listener) {
    this.connectionListeners.add(listener);
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
    reportError(new ConnectionException(errorCode));
  }

  private void reportError(ConnectionException error) {
    for (ConnectionListener listener : connectionListeners) {
      listener.onConnectionError(error);
    }
  }

  private void rejectMessage(JSObject message) {
    close();
    for (ConnectionListener listener : connectionListeners) {
      listener.onMessageRejected(message);
    }
  }

  protected long getMessageNumberCounter() {
    return messageNumberCounter.get();
  }

  public String getAppName() {
    return sessionPolicy.getSessionData().getAppName();
  }

  public String getSessionId() {
    return sessionPolicy.getSessionData().getSessionId();
  }

  public AbstractSocket getTransport() {
    return transport;
  }

  public ConnectionState getState() {
    return state;
  }

  public SessionPolicy getSessionPolicy() {
    return sessionPolicy;
  }

  public void setSessionPolicy(SessionPolicy sessionPolicy) {
    this.sessionPolicy = sessionPolicy;
  }
}
