package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSTypes.JSElements;
import com.metarhia.jstp.exceptions.AlreadyConnectedException;
import com.metarhia.jstp.exceptions.MessageHandlingException;
import com.metarhia.jstp.messagehandling.MessageHandler;
import com.metarhia.jstp.messagehandling.MessageHandlerImpl;
import com.metarhia.jstp.session.SessionData;
import com.metarhia.jstp.session.SessionPolicy;
import com.metarhia.jstp.session.SimpleSessionPolicy;
import com.metarhia.jstp.storage.StorageInterface;
import com.metarhia.jstp.transport.Transport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connection that uses JSTP over specified transport to transmit data
 */
public class Connection implements
    Transport.TransportListener,
    MessageHandler.MessageHandlerListener {

  private static final Logger logger = LoggerFactory.getLogger(Connection.class);

  private static AtomicLong nextConnectionID = new AtomicLong(0);

  private long id;

  private ConnectionState state;

  private final Object stateLock;

  /**
   * Transport to send/receive messages
   */
  private Transport transport;

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

  private MessageHandler messageHandler;

  private AtomicLong messageNumberCounter;

  /**
   * Creates connection instance over specified transport {@param transport}.
   * and with {@link SimpleSessionPolicy} as default {@link SessionPolicy},
   * and {@link MessageHandlerImpl} as default {@link MessageHandler}.
   *
   * @param transport transport to be used for sending JSTP data
   */
  public Connection(Transport transport) {
    this(transport, new SimpleSessionPolicy());
  }

  /**
   * Creates connection instance over specified transport with specified session policy
   * and {@link MessageHandlerImpl} as default {@link MessageHandler}.
   *
   * @param transport     transport to be used for sending JSTP messages
   * @param sessionPolicy session policy
   */
  public Connection(Transport transport, SessionPolicy sessionPolicy) {
    this(transport, sessionPolicy, new MessageHandlerImpl());
  }

  public Connection(Transport transport, SessionPolicy sessionPolicy,
                    MessageHandler messageHandler) {
    this.id = nextConnectionID.getAndIncrement();
    this.state = ConnectionState.AWAITING_HANDSHAKE;
    this.stateLock = new Object();
    this.messageNumberCounter = new AtomicLong(0);

    setTransport(transport);

    setMessageHandler(messageHandler);

    setSessionPolicy(sessionPolicy);

    this.connectionListeners = new CopyOnWriteArrayList<>();
    this.eventHandlers = new ConcurrentHashMap<>();
    this.clientMethodNames = new ConcurrentHashMap<>();
    this.handlers = new ConcurrentHashMap<>();
    this.callHandlers = new ConcurrentHashMap<>();
  }

  /**
   * Calls {@link #useTransport(Transport, boolean)} with connect set to true
   */
  public void useTransport(Transport transport) {
    useTransport(transport, true);
  }

  /**
   * Changes transport used by a connection. It closes the previous transport if it was present,
   * and establishes connection via {@link #connect(String)} if Application was provided
   * before.
   *
   * @param transport new transport to be used for sending JSTP messages
   * @param connect   if true it will try to {@link #connect(AppData)} if applicable
   */
  public void useTransport(Transport transport, boolean connect) {
    synchronized (stateLock) {
      if (this.transport != null) {
        this.transport.setListener(null);
        this.transport.close(true);
      }

      setTransport(transport);
      this.state = ConnectionState.AWAITING_HANDSHAKE;
    }

    if (connect && getAppName() != null) {
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
   * Checks if transport is connected, if it is not calls {@link Transport#connect()}
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
      synchronized (stateLock) {
        state = ConnectionState.AWAITING_HANDSHAKE;
      }
      transport.connect();
    } else {
      onTransportConnected();
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
    messageHandler.clearQueue();
    setMessageNumberCounter(0);
    long messageNumber = getNextMessageNumber();
    if (handler != null) {
      handlers.put(messageNumber, handler);
    } else {
      handlers.remove(messageNumber);
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
    long messageNumber = 0;
    if (handler != null) {
      handlers.put(messageNumber, handler);
    } else {
      handlers.remove(messageNumber);
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

  /**
   * Sends a call message over the connection. If session is preserved this call is guaranteed
   * to be received by the other party but this doesn't mean that you will always receive
   * a callback.
   * If you need to get the callback please refer to
   * {@link #callResendable(String, String, List, ManualHandler)} method or resend manually
   * upon {@link ConnectionError#CALLBACK_LOST} error.
   *
   * @param interfaceName name of an interface
   * @param methodName    name of a method
   * @param args          call arguments
   * @param handler       callback that will be called when appropriate callback message is
   *                      received
   */
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

  /**
   * Sends a call message over the connection resending it if is not possible to
   * get a callback (resends upon receiving
   * {@link ConnectionError#CALLBACK_LOST} error).
   *
   * @param interfaceName name of an interface
   * @param methodName    name of a method
   * @param args          call arguments
   * @param handler       callback that will be called when appropriate callback message is
   *                      received
   */
  public void callResendable(final String interfaceName,
                             String methodName,
                             List<?> args,
                             final ManualHandler handler) {
    long messageNumber = getNextMessageNumber();
    final Message callMessage = new Message(messageNumber, MessageType.CALL)
        .putArg(methodName, args)
        .addProtocolArg(interfaceName);

    if (handler != null) {
      handlers.put(messageNumber, new ManualHandler() {
        @Override
        public void onMessage(JSObject message) {
          handler.onMessage(message);
        }

        @Override
        public void onError(int errorCode) {
          if (errorCode == ConnectionError.CALLBACK_LOST.getErrorCode()) {
            long messageNumber = getNextMessageNumber();
            callMessage.setMessageNumber(messageNumber);
            handlers.put(messageNumber, this);
            sendBuffered(callMessage);
          } else {
            handler.onError(errorCode);
          }
        }
      });
    }

    sendBuffered(callMessage);
  }

  /**
   * Sends a callback message
   *
   * @param result callback result ({@link JSCallback#OK} or {@link JSCallback#ERROR})
   * @param args   callback parameters
   */
  public void callback(JSCallback result, List<?> args) {
    callback(result, args, null);
  }

  /**
   * Sends a callback message with a specific message number {@param messageNumber}
   *
   * @param result        callback result ({@link JSCallback#OK} or {@link JSCallback#ERROR})
   * @param args          callback parameters
   * @param messageNumber message number for callback
   */
  public void callback(JSCallback result, List<?> args, Long messageNumber) {
    if (messageNumber == null) {
      messageNumber = getNextMessageNumber();
    }

    Message callbackMessage = new Message(messageNumber, MessageType.CALLBACK)
        .putArg(result.toString(), args);

    send(callbackMessage);
  }

  /**
   * Sends an inspect message
   *
   * @param interfaceName interface name to inspect
   * @param handler       callback handler
   */
  public void inspect(String interfaceName, ManualHandler handler) {
    long messageNumber = getNextMessageNumber();
    Message inspectMessage = new Message(messageNumber, MessageType.INSPECT)
        .addProtocolArg(interfaceName);

    if (handler != null) {
      handlers.put(messageNumber, handler);
    }

    sendBuffered(inspectMessage);
  }

  /**
   * Sends an event message
   *
   * @param interfaceName name of an interface
   * @param eventName     name of the event
   * @param args          event parameters
   */
  public void event(String interfaceName, String eventName, List<?> args) {
    long messageNumber = getNextMessageNumber();
    Message eventMessage = new Message(messageNumber, MessageType.EVENT)
        .putArg(eventName, args)
        .addProtocolArg(interfaceName);

    sendBuffered(eventMessage);
  }

  public void ping(ManualHandler handler) {
    long messageNumber = getNextMessageNumber();
    Message pingMessage = new Message(messageNumber, MessageType.PING);

    if (handler != null) {
      handlers.put(messageNumber, handler);
    }

    sendBuffered(pingMessage);
  }

  public void pong(long messageNumber) {
    Message pongMessage = new Message(messageNumber, MessageType.PONG);
    send(pongMessage);
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
  public void onMessageParsed(JSObject message) {
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

  @Override
  public void onHandlingError(MessageHandlingException e) {
    logger.info("Message handling error", e);
    if (transport.isConnected()) {
      transport.close(true);
    }
    messageHandler.clearQueue();
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

      ManualHandler handshakeHandler = handlers.remove(0L);

      final String payloadKey = message.getKey(1);
      final Object payload = message.get(payloadKey);
      if (JSCallback.fromString(payloadKey) == JSCallback.ERROR) {
        int errorCode = ((List<Integer>) payload).get(0);
        close(true);
        if (handshakeHandler != null) {
          handshakeHandler.onError(errorCode);
        } else {
          logger.info("Handshake failed with error {}", errorCode);
        }
      } else if (transport.isConnected()) {
        // make sure transport is still connected to avoid extra work
        state = ConnectionState.CONNECTED;
        boolean restored = false;
        if (payload instanceof Number) {
          restored = true;
          processHandshakeRestoreResponse(message);
        } else if (payload instanceof String) {
          processHandshakeResponse(message);
        } else {
          rejectMessage(message, true);
          return false;
        }
        reportConnected(restored);

        if (handshakeHandler != null) {
          handshakeHandler.onMessage(JSElements.EMPTY_OBJECT);
        }
      }
    }
    return true;
  }

  private void processHandshakeRestoreResponse(JSObject message) {
    long numServerReceivedMessages = ((Number) message.get(JSCallback.OK.toString())).longValue();
    setMessageNumberCounter(sessionPolicy.getSessionData().getNumSentMessages() + 1);
    sessionPolicy.restore(numServerReceivedMessages);
  }

  private void processHandshakeResponse(JSObject message) {
    String sessionId = (String) message.get(JSCallback.OK.toString());
    setMessageNumberCounter(1);
    Map<Long, ManualHandler> oldHandlers = handlers;
    handlers = new ConcurrentHashMap<>();
    sessionPolicy.onNewConnection(getAppData(), sessionId, oldHandlers);
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
      handler.onMessage(message);
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
      callbackHandler.onMessage(message);
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
      eh.onMessage(message);
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
    pong(pingNumber);
    return true;
  }

  private boolean pongMessageHandler(JSObject message) {
    long receiverIndex = getMessageNumber(message);
    ManualHandler pongHandler = handlers.remove(receiverIndex);
    if (pongHandler != null) {
      pongHandler.onMessage(JSElements.EMPTY_OBJECT);
    }
    return true;
  }

  @Override
  public void onTransportConnected() {
    synchronized (stateLock) {
      if (getAppName() == null
          || state != ConnectionState.AWAITING_RECONNECT
          && state != ConnectionState.AWAITING_HANDSHAKE) {
        return;
      }
      sessionPolicy.onTransportAvailable();
    }
  }

  @Override
  public void onMessageReceived(String message) {
    messageHandler.post(message);
  }

  @Override
  public void onTransportClosed() {
    synchronized (stateLock) {
      if (state == ConnectionState.CLOSING) {
        state = ConnectionState.CLOSED;
        reportClosed();
      } else if (getAppName() != null) {
        if (state != ConnectionState.AWAITING_RECONNECT) {
          state = ConnectionState.AWAITING_RECONNECT;
          reportClosed();
        }
      } else {
        if (state != ConnectionState.AWAITING_HANDSHAKE) {
          state = ConnectionState.AWAITING_HANDSHAKE;
          reportClosed();
        }
      }
      sessionPolicy.onConnectionClosed();
      transport.clearQueue();
    }
  }

  @Override
  public void onTransportError(Exception e) {
    logger.info("Transport error", e);
    if (!(e instanceof AlreadyConnectedException) && transport.isConnected()) {
      transport.close(true);
    }
  }

  /**
   * Sets call handler for incoming calls with interface name {@param interfaceName} and method
   * name {@param methodName}
   *
   * @param interfaceName interface name of a method
   * @param methodName    name of the method
   * @param callHandler   handler for incoming call
   */
  public void setCallHandler(String interfaceName, String methodName, ManualHandler callHandler) {
    Map<String, ManualHandler> interfaceHandlers = callHandlers.get(interfaceName);
    if (interfaceHandlers == null) {
      callHandlers.put(interfaceName, new HashMap<String, ManualHandler>());
      interfaceHandlers = callHandlers.get(interfaceName);
    }
    interfaceHandlers.put(methodName, callHandler);
  }

  /**
   * Removes call handler for incoming call with interface name {@param interfaceName} and method
   * name {@param methodName}
   *
   * @param interfaceName interface name of the method
   * @param methodName    name of the method
   */
  public void removeCallHandler(String interfaceName, String methodName) {
    final Map<String, ManualHandler> interfaceHandlers = callHandlers.get(interfaceName);
    if (interfaceHandlers != null) {
      interfaceHandlers.remove(methodName);
    }
  }

  /**
   * Adds event handler for event with interface name {@param interfaceName} and name
   * {@param eventName}
   *
   * @param interfaceName interface name of the event
   * @param eventName     name of the event
   * @param handler       event handler
   */
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

  /**
   * Removes event handler for event with interface name {@param interfaceName} and name
   * {@param eventName}
   *
   * @param interfaceName interface name of the event
   * @param eventName     name of the event
   * @param handler       event handler
   */
  public void removeEventHandler(String interfaceName, String eventName, ManualHandler handler) {
    Map<String, List<ManualHandler>> ehs = eventHandlers.get(interfaceName);
    if (ehs != null) {
      List<ManualHandler> eventHandlers = ehs.get(eventName);
      if (eventHandlers != null) {
        eventHandlers.remove(handler);
      }
    }
  }

  /**
   * Adds handler for message with number {@param messageNumber}
   *
   * @param messageNumber number of the message to handle
   * @param handler       handler of the incoming message
   */
  public void addHandler(long messageNumber, ManualHandler handler) {
    handlers.put(messageNumber, handler);
  }

  public ManualHandler removeHandler(long messageNumber) {
    return handlers.remove(messageNumber);
  }

  public Map<Long, ManualHandler> getHandlers() {
    return handlers;
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

  public long getNextMessageNumber() {
    return messageNumberCounter.getAndIncrement();
  }

  private long setMessageNumberCounter(long messageNumber) {
    messageNumberCounter.set(messageNumber);
    return messageNumber;
  }

  /**
   * Closes connection.
   *
   * @see #close(boolean)
   */
  public void close() {
    close(false);
  }

  /**
   * Closes connection
   *
   * @param forced if true the connection should be closed immediately, otherwise it may perform
   *               additional operations, such as writing messages remaining in the queue or
   *               handling remaining incoming data
   */
  public void close(boolean forced) {
    synchronized (stateLock) {
      if (state == ConnectionState.CLOSING) {
        return;
      }
      state = ConnectionState.CLOSING;
      if (transport != null && transport.isConnected()) {
        transport.close(forced);
      } else {
        onTransportClosed();
      }
    }
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

  /**
   * Wrapper method for {@link #setClientMethodNames(String, List)}
   *
   * @param interfaceName interface name of client methods
   * @param names         client method names
   */
  public void setClientMethodNames(String interfaceName, String... names) {
    setClientMethodNames(interfaceName, Arrays.asList(names));
  }

  /**
   * Sets client method names for incoming inspect messages
   *
   * @param interfaceName interface name of client methods
   * @param names         client method names
   */
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

  public ConnectionListener setReconnectCallback(final ReconnectCallback reconnectCallback) {
    SimpleConnectionListener reconnectListener = new SimpleConnectionListener() {
      @Override
      public void onConnectionClosed() {
        reconnectCallback.onConnectionLost(Connection.this, new TransportConnector() {
          @Override
          public void connect(Transport newTransport) {
            if (newTransport == null) {
              getTransport().connect();
            } else {
              useTransport(newTransport);
            }
          }
        });
      }
    };
    addListener(reconnectListener);
    return reconnectListener;
  }

  /**
   * Checks if connection is established
   *
   * @return true if connection is established (and handshake has been performed)
   * and false otherwise
   */
  public boolean isConnected() {
    return state == ConnectionState.CONNECTED;
  }

  /**
   * Checks if connection is closed
   *
   * @return true if connection is closed or closing and false otherwise
   */
  public boolean isClosed() {
    return state == ConnectionState.CLOSED || state == ConnectionState.CLOSING;
  }

  /**
   * Gets connection ID
   *
   * @return connection ID
   */
  public long getId() {
    return id;
  }

  /**
   * Adds listener to the connection events
   *
   * @param listener connection events listener
   */
  public void addListener(ConnectionListener listener) {
    this.connectionListeners.add(listener);
  }

  public void removeListener(ConnectionListener listener) {
    this.connectionListeners.remove(listener);
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

  private void rejectMessage(JSObject message, boolean fatal) {
    if (fatal) {
      close();
    }
    for (ConnectionListener listener : connectionListeners) {
      listener.onMessageRejected(message);
    }
  }

  public long getMessageNumberCounter() {
    return messageNumberCounter.get();
  }

  public AppData getAppData() {
    return sessionPolicy.getSessionData().getAppData();
  }

  public String getAppName() {
    return getAppData().getName();
  }

  public String getSessionId() {
    return sessionPolicy.getSessionData().getSessionId();
  }

  public void setAppData(AppData appData) {
    sessionPolicy.getSessionData().setAppData(appData);
  }

  public void setSessionId(String sessionId) {
    sessionPolicy.getSessionData().setSessionId(sessionId);
  }

  public Transport getTransport() {
    return transport;
  }

  public ConnectionState getState() {
    return state;
  }

  public SessionPolicy getSessionPolicy() {
    return sessionPolicy;
  }

  public MessageHandler getMessageHandler() {
    return messageHandler;
  }

  /**
   * Changes message handler used by the connection to {@param messageHandler} while disregarding
   * previously used one. Be aware that this method makes no guarantees that all of the messages
   * of the previous message handler have been processed.
   *
   * @param messageHandler new message handler
   */
  public void setMessageHandler(MessageHandler messageHandler) {
    if (this.messageHandler != null) {
      this.messageHandler.setListener(null);
    }
    this.messageHandler = messageHandler;
    this.messageHandler.setListener(this);
  }

  /**
   * Simple transport setter, will not ensure any transport or state transition.
   * Refer to {@link #useTransport(Transport, boolean)} for those features.
   *
   * @param transport new transport
   */
  public void setTransport(Transport transport) {
    this.transport = transport;
    this.transport.setListener(this);
  }

  public void setSessionPolicy(SessionPolicy sessionPolicy) {
    synchronized (stateLock) {
      // guard by stateLock to avoid setting it to null in the middle of session restore
      if (this.sessionPolicy != null) {
        this.sessionPolicy.setConnection(null);
      }
      this.sessionPolicy = sessionPolicy;
      this.sessionPolicy.setConnection(this);
    }
  }

  public interface ReconnectCallback {

    /**
     * Will be called upon transport connection loss to try to restore the connection.
     * Should use {@param transportConnector} as needed to do so.
     *
     * @param connection         instance that has lost a connection
     * @param transportConnector connector to be used to establish a new transport
     */
    void onConnectionLost(Connection connection, TransportConnector transportConnector);
  }

  public interface TransportConnector {

    /**
     * Called when new transport connection can be established
     *
     * @param newTransport either new transport or null upon which it will try to
     *                     reconnect to the same address
     */
    void connect(Transport newTransport);
  }
}
