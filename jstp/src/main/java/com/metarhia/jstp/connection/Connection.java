package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.exceptions.AlreadyConnectedException;
import com.metarhia.jstp.exceptions.ConnectionException;
import com.metarhia.jstp.exceptions.MessageHandlingException;
import com.metarhia.jstp.storage.StorageInterface;
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

  private static final Logger logger = LoggerFactory.getLogger(Connection.class);

  private static AtomicLong nextConnectionID = new AtomicLong(0);

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

    setSessionPolicy(sessionPolicy);

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

    if (getAppName() != null) {
      connect(getAppData());
    }
  }

  /**
   * Calls {@link #connect(AppData)} with {@param app} parsed
   * via {@link AppData#valueOf(String)} and null for sessionID
   *
   * @param app application to connect to as 'name' or 'name@version'
   *            where version is a valid semver version or range
   *            (must not be null)
   */
  public void connect(String app) {
    connect(AppData.valueOf(app));
  }

  /**
   * Calls {@link #connect(AppData, String)} with null for sessionID
   */
  public void connect(AppData appData) {
    connect(appData, null);
  }

  /**
   * Calls {@link #connect(AppData, String)} with {@param app} parsed
   * via {@link AppData#valueOf(String)} and null for sessionID
   *
   * @param app application to connect to as 'name' or 'name@version'
   *            where version is a valid semver version or range
   *            (must not be null)
   */
  public void connect(String app, String sessionID) {
    connect(AppData.valueOf(app), sessionID);
  }

  /**
   * Checks if transport is connected, if it is not calls {@link AbstractSocket#connect()}
   * otherwise initiates a handshake
   *
   * @param appData   data of the application to connect to (must not be null)
   * @param sessionID optional id to restore session
   */
  public void connect(AppData appData, String sessionID) {
    if (appData == null) {
      throw new RuntimeException("Application must not be null");
    }
    sessionPolicy.getSessionData().setParameters(appData, sessionID);
    if (!transport.isConnected()) {
      transport.connect();
    } else {
      onConnected();
    }
  }

  /**
   * Sends anonymous handshake message
   *
   * @param app     application to connect to as 'name' or 'name@version'
   *                where version is a valid semver version or range
   *                (must not be null)
   * @param handler optional handler that will be called when response handshake message comes from
   *                the server
   */
  public void handshake(String app, ManualHandler handler) {
    handshake(AppData.valueOf(app), handler);
  }

  /**
   * Sends anonymous handshake message
   *
   * @param appData data of the application to connect to (must not be null)
   * @param handler optional handler that will be called when response handshake message comes from
   *                the server
   */
  public void handshake(AppData appData, ManualHandler handler) {
    handshake(appData, null, null, handler);
  }

  /**
   * Sends handshake message with authorization
   *
   * @param app      application to connect to as 'name' or 'name@version'
   *                 where version is a valid semver version or range
   *                 (must not be null)
   * @param username login for authorization on server
   * @param password password for authorization on server
   * @param handler  optional handler that will be called when response handshake message comes from
   *                 the server
   */
  public void handshake(String app, String username, String password, ManualHandler handler) {
    if (app == null) {
      throw new RuntimeException("Application must not be null");
    }
    handshake(AppData.valueOf(app), username, password, handler);
  }

  public void handshake(AppData appData, String username, String password, ManualHandler handler) {
    if (appData == null) {
      throw new RuntimeException("Application must not be null");
    }
    sessionPolicy.getSessionData().setParameters(appData, null);
    setMessageNumberCounter(0);
    long messageNumber = getNextMessageNumber();
    if (handler != null) {
      handlers.put(messageNumber, handler);
    }

    Message hm = new Message(messageNumber, MessageType.HANDSHAKE)
        .addProtocolArg(appData.getName());
    if (appData.getVersion() != null) {
      hm.addProtocolArg(appData.getVersion());
    }
    if (username != null && password != null) {
      hm.putArg(username, password);
    }
    sendHandshake(hm.stringify());
  }

  /**
   * Tries to restore session with id {@param sessionID}
   *
   * @param app       application to connect to as 'name' or 'name@version'
   *                  where version is a valid semver version or range
   *                  (must not be null)
   * @param sessionID id of a session to restore
   * @param handler   optional handler that will be called when response handshake message comes
   *                  from the server
   */
  public void handshake(String app, String sessionID, ManualHandler handler) {
    if (app == null) {
      throw new RuntimeException("Application must not be null");
    }
    handshake(AppData.valueOf(app), sessionID, handler);
  }

  /**
   * Tries to restore session with {@param sessionID}
   *
   * @param appData   data of the application to connect to (must not be null)
   * @param sessionID id of a session to restore
   * @param handler   optional handler that will be called when response handshake message comes
   *                  from the server
   */
  public void handshake(AppData appData, String sessionID, ManualHandler handler) {
    sessionPolicy.getSessionData().setParameters(appData, sessionID);
    setMessageNumberCounter(sessionPolicy.getSessionData().getNumSentMessages());
    long messageNumber = 0;
    if (handler != null) {
      handlers.put(messageNumber, handler);
    }

    Message hm = new Message(messageNumber, MessageType.HANDSHAKE)
        .addProtocolArg(appData.getName())
        .putArgs("session", sessionID,
            sessionPolicy.getSessionData().getNumReceivedMessages());
    if (appData.getVersion() != null) {
      hm.addProtocolArg(appData.getVersion());
    }

    sendHandshake(hm.stringify());
  }

  private void sendHandshake(String handshake) {
    synchronized (stateLock) {
      if (transport.isConnected() &&
          (state == ConnectionState.AWAITING_HANDSHAKE
              || state == ConnectionState.AWAITING_RECONNECT)) {
        state = ConnectionState.AWAITING_HANDSHAKE_RESPONSE;
        transport.send(handshake);
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
      transport.send(message);
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
      if (messageType != MessageType.HANDSHAKE && state != ConnectionState.CONNECTED
          || messageType == null) {
        rejectMessage(message, true);
      } else if (handleMessage(message, messageType)) {
        sessionPolicy.onMessageReceived(message, messageType);
      }
    } catch (ClassCastException | NullPointerException | MessageHandlingException e) {
      // means message was ill formed
      rejectMessage(message, true);
    }
  }

  private boolean handleMessage(JSObject message, MessageType type) {
    switch (type) {
      case HANDSHAKE:
        return handshakeMessageHandler(message);
      case CALL:
        return callMessageHandler(message);
      case CALLBACK:
        return callbackMessageHandler(message);
      case EVENT:
        return eventMessageHandler(message);
      case INSPECT:
        return inspectMessageHandler(message);
      case PING:
        return pingMessageHandler(message);
      case PONG:
        return pongMessageHandler(message);
      default:
        rejectMessage(message, false);
        return false;
    }
  }

  private boolean handshakeMessageHandler(JSObject message) {
    synchronized (stateLock) {
      if (state != ConnectionState.AWAITING_HANDSHAKE_RESPONSE) {
        // if transport is not connected it means that the connection was closed before
        // this method was called so don't report this
        if (transport.isConnected()) {
          rejectMessage(message, true);
        }
        return false;
      }

      final String payloadKey = message.getKey(1);
      final Object payload = message.get(payloadKey);
      if (JSCallback.fromString(payloadKey) == JSCallback.ERROR) {
        int errorCode = ((List<Integer>) payload).get(0);
        reportError(errorCode);
        close(true);
      } else if (transport.isConnected()) {
        // make sure transport is still connected to avoid extra work
        state = ConnectionState.CONNECTED;
        boolean restored = false;
        if (payload instanceof Number) {
          processHandshakeRestoreResponse(message);
          setMessageNumberCounter(sessionPolicy.getSessionData().getNumSentMessages() + 1);
          restored = true;
        } else if (payload instanceof String) {
          processHandshakeResponse(message);
        } else {
          rejectMessage(message, true);
          return false;
        }
        reportConnected(restored);
      }
    }
    return true;
  }

  private void processHandshakeRestoreResponse(JSObject message) {
    if (getMessageNumberCounter() == 0) {
      getNextMessageNumber();
    }
    long numServerReceivedMessages = ((Number) message.get(JSCallback.OK.toString())).longValue();
    sessionPolicy.restore(numServerReceivedMessages);

    long receiverIndex = 0;
    ManualHandler callbackHandler = handlers.remove(receiverIndex);
    if (callbackHandler != null) {
      callbackHandler.handle(message);
    }

  }

  private void processHandshakeResponse(JSObject message) {
    long receiverIndex = 0;
    ManualHandler handshakeHandler = handlers.remove(receiverIndex);

    // always reset connection on basic handshake
    reset();
    // count first handshake
    getNextMessageNumber();

    sessionPolicy.getSessionData().setSessionId((String) message.get(JSCallback.OK.toString()));

    if (handshakeHandler != null) {
      handshakeHandler.handle(message);
    }
  }

  private boolean callMessageHandler(JSObject message) {
    String interfaceName = getInterfaceName(message);
    if (isSecondNotArray(message)) {
      rejectMessage(message, true);
      return false;
    }
    String methodName = message.getKey(1);

    final Map<String, ManualHandler> iface = callHandlers.get(interfaceName);
    if (iface == null) {
      return true;
    }

    ManualHandler handler = iface.get(methodName);
    if (handler != null) {
      handler.handle(message);
    }
    return true;
  }

  private boolean callbackMessageHandler(JSObject message) {
    long receiverIndex = getMessageNumber(message);
    if (isSecondNotArray(message)) {
      rejectMessage(message, true);
      return false;
    }
    ManualHandler callbackHandler = handlers.remove(receiverIndex);
    if (callbackHandler != null) {
      callbackHandler.handle(message);
    }
    return true;
  }

  private boolean eventMessageHandler(JSObject message) {
    String interfaceName = getInterfaceName(message);

    if (isSecondNotArray(message)) {
      rejectMessage(message, true);
      return false;
    }

    Map<String, List<ManualHandler>> interfaceHandlers = eventHandlers.get(interfaceName);
    if (interfaceHandlers == null) {
      return true;
    }

    String eventName = message.getKey(1);
    List<ManualHandler> eventHandlers = interfaceHandlers.get(eventName);
    if (eventHandlers == null) {
      return true;
    }

    for (ManualHandler eh : eventHandlers) {
      eh.handle(message);
    }
    return true;
  }

  private boolean inspectMessageHandler(JSObject message) {
    long messageCounter = getMessageNumber(message);
    String interfaceName = getInterfaceName(message);
    List<String> methods = clientMethodNames.get(interfaceName);
    if (methods != null) {
      callback(JSCallback.OK, methods, messageCounter);
    } else {
      callback(JSCallback.ERROR,
          Collections.singletonList(ConnectionError.INTERFACE_NOT_FOUND.getErrorCode()),
          messageCounter);
    }
    return true;
  }

  private boolean pingMessageHandler(JSObject message) {
    long pingNumber = getMessageNumber(message);
    Message pongMessage = new Message(pingNumber, MessageType.PONG);
    sendBuffered(pongMessage);
    return true;
  }

  private boolean pongMessageHandler(JSObject message) {
    return callbackMessageHandler(message);
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

  public void removeHandler(long messageNumber) {
    handlers.remove(messageNumber);
  }

  public void saveSession(StorageInterface storageInterface) {
    sessionPolicy.saveSession(storageInterface);
  }

  public SessionData getSessionData() {
    return sessionPolicy.getSessionData();
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

  private void reset(String app, long messageCounter) {
    setMessageNumberCounter(messageCounter);
    handlers = new ConcurrentHashMap<>();
    sessionPolicy.reset(app);
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
      if (transport != null && transport.isConnected()) {
        transport.close(forced);
      } else {
        onSocketClosed();
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
    return (String) ((List<Object>) message.getByIndex(0)).get(1);
  }

  public static long getMessageNumber(JSObject message) {
    return ((Number) ((List<Object>) message.getByIndex(0)).get(0)).longValue();
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
      sessionPolicy.onTransportAvailable();
    }
    // todo add calls and fields for username\password auth
  }

  @Override
  public void onSocketClosed() {
    synchronized (stateLock) {
      if (state == ConnectionState.CLOSING) {
        state = ConnectionState.CLOSED;
        reportClosed();
      } else if (getAppName() != null) {
        state = ConnectionState.AWAITING_RECONNECT;
        reportClosed();
      } else {
        state = ConnectionState.AWAITING_HANDSHAKE;
        // app name is null -> connection was not established yet, so don't report
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

  private void rejectMessage(JSObject message, boolean fatal) {
    if (fatal) {
      close();
    }
    for (ConnectionListener listener : connectionListeners) {
      listener.onMessageRejected(message);
    }
  }

  protected long getMessageNumberCounter() {
    return messageNumberCounter.get();
  }

  public AppData getAppData() {
    return sessionPolicy.getSessionData().getAppData();
  }

  public String getAppName() {
    return sessionPolicy.getSessionData().getAppData().getName();
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
    this.sessionPolicy.setConnection(this);
  }
}
