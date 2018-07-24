# Java JSTP implementation

[jstp js](https://github.com/metarhia/jstp)

[Check for the latest JSTP version](https://bintray.com/metarhia/maven/jstp)

## Installation
Gradle:

Add this to your build.gradle (check for the latest version):
```
dependencies {
  compile group: 'com.metarhia.jstp', name: 'jstp', version: '0.10.0'
}
```

Maven:
```
<dependency>
  <groupId>com.metarhia.jstp</groupId>
  <artifactId>jstp</artifactId>
  <version>0.10.0</version>
  <type>pom</type>
</dependency>
```

## Parser usage

`JSParser` will mostly convert to native java objects
(with a few exceptions where specific implementation was needed)

Few simple examples
```java
JSObject a = JSParser.parse("{a: 3, b: 2}");
// Get field 'a' of object {a: 3, b: 2}
a.get("a"); // returns 3

// Get second field of object {a: 3, b: 2}
a.getByIndex(1); // returns 2

// But you can easily use it as a Map
Map<String, Number> map = (Map<String, Number>) a;
// or
Map<String, Number> a = JSParser.parse("{a: 3, b: 2}");


List<Double> arr = JSParser.parse("[1, 2, 3]");
// Get second element of array [1, 2, 3];
arr.get(1); // returns 2


String str = JSParser.parse("'abc'");
// str now equals to "abc"


List<?> arr = JSParser.parse("[1,, 3]");
// Get second element of array [1,, 3];
arr.get(1); // returns JSUndefined
```

To serialize objects you can use `JSSerializer`
```java
List<Number> arr = new JSNativeParser("[1, 2, 3]").parse();
JSSerializer.stringify(arr); // returns "[1,2,3]"


Map<String, Number> a = JSParser.parse("{a: 3, b: 2}");
JSSerializer.stringify(a); // returns "{a:3,b:2}"
```

If it doesn't know how to serialize input it'll be serialized as `"undefined"`,
also you can define how your objects will be serialized via `JSSerializable`
interface.

They can be parsed in js with a simple eval statement or
[jstp](https://github.com/metarhia/jstp) and [mdsf](https://github.com/metarhia/mdsf)

## Connection

### Establish connection

To establish JSTP connection, you need to provide transport.
As of now the only available transport is TCP. Optionally you can
define session policy (there are 2 basic ones in SDK:
`DropSessionPolicy` - which will create new connection every
time transport is restored and `SimpleSessionPolicy` - which
will resend cached messages and try to restore session.
`SimpleSessionPolicy` is used by default). For example:

```java
String host = "metarhia.com";
int port = 80;

Transport transport = new TCPTransport(host, port /*, useSSl == true */);
Connection connection = new Connection(transport);
```

You can change used transport by calling `useTransport()` method.
This will close previous transport if available and set provided one
as current transport. It will try to connect and upon connection
appropriate method of session policy will be called when transport
reports that it's connected.

To react to connection events, you can use `ConnectionListener` (there is
also `SimpleConnectionListener` version that defaults to ignoring all calls):

```java
connection.addListener(new ConnectionListener() {

  @Override
  public void onConnected(boolean restored) {
    // ...
  }

  @Override
  public void onConnectionClosed() {
    // ...
  }

  @Override
  public void onMessageRejected(JSObject message) {
    // ...
  }
});
```

You can define applicationName and/or session Id when connecting,
or connect without them (you must at least once call `connect`
with application name before that):
```java
connection.connect();
// ...
connection.connect("applicationName");
// ...
connection.connect("applicationName", "sessionId");
```

### JSTP message types

#### Handshake

Usually you don't have to send handshake messages manually. You may need
them if If you need to implement your own session policy or change
transport on active connection. You can send `handshake` message as follows:

```java
// anonymous handshake message
connection.handshake("applicationName", new ManualHandler() {

  @Override
  public void onMessage(JSObject message) {
    // ...
  }

  @Override
  public void onError(int errorCode) {
    // ...
  }
});

// handshake with attempt to restore session "sessionId"
connection.handshake("applicationName", "sessionId", new ManualHandler() {

  @Override
  public void onMessage(JSObject message) {
    // ...
  }

  @Override
  public void onError(int errorCode) {
    // ...
  }
});

// handshake message with authorization (login and password)
connection.handshake("applicationName", "name", "pass", new ManualHandler() {

  @Override
  public void onMessage(JSObject message) {
    // ...
  }

  @Override
  public void onError(int errorCode) {
    // ...
  }
});

```

#### Call

To send `call` message:

```java
List<?> args = Arrays.asList('a', 'b', 'c');
connection.call("interfaceName", "methodName", args, new ManualHandler() {

  @Override
  public void onMessage(JSObject message) {
    // ...
  }

  @Override
  public void onError(int errorCode) {
    // ...
  }
);
```

Or you can use `OkErrorHandler` that will make this much simpler and clearer:

```java
List<?> args = Arrays.asList('a', 'b', 'c');
connection.call("interfaceName", "methodName", args, new OkErrorHandler() {

  @Override
  public void handleOk(List<?> data) {
    // ...
  }

  @Override
  public void handleError(Integer errorCode, List<?> data) {
    // ...
  }
);
```

To handle incoming `call` messages, you have to set `setCallHandler()` for that call.
There can only be one call handler for each call.

#### Callback

While sending callback you should specify callback type (`JSCallback.OK` or
`JSCallback.ERROR`) and arbitrary arguments.

```java
connection.setCallHandler("interfaceName", "methodName", new CallHandler() {

  @Override
  public void handleCall(String methodName, List<?> data) {
    // ...
    callback(connection, JSCallback.OK, Arrays.asList('Hello'));
  }
});
```

You also can send `callback` messages like this:

```java
connection.callback(JSCallback.OK, args);

// define custom message number
Long customIndex;
// ...
connection.callback(JSCallback.OK, args, customIndex);

```

#### Inspect

Incoming inspect messages are handled by the Connection itself. To make
methods visible through inspect message you just need to define method
names with appropriate interfaces.

```java
connection.setClientMethodNames("interfaceName1", "methodName1", "methodName2");
connection.setClientMethodNames("interfaceName2", "methodName1", "methodName2");
// ...
```

To send `inspect` message:
```java
connection.inspect("interfaceName", new ManualHandler() {

  @Override
  public void onMessage(JSObject message) {
    // ...
  }

  @Override
  public void onError(int errorCode) {
    // ...
  }
});
```

#### Event

To handle incoming events, you add event handlers with `addEventHandler()`.
There can be multiple event handlers for each event.

```java
connection.addEventHandler("interfaceName", "methodName", new EventHandler() {

  @Override
  public void handleEvent(String eventName, List<?> data) {
    // ...
  }
});
```

Sending `event` message:
```java
List<String> args = Arrays.asList('Hello');
connection.event("interfaceName", "methodName", args);
```

### Executable handler

If you want to handle incoming packets on a separate thread, you can use
`ExecutableHandler` instead of `ManualHandler`. It requires `Executor` to run
the packet handling method on it. Incoming `message` is a protected parameter in
ExecutableHandler. You can use it like this:

```java
Connection connection = ...;
Executor executor = ...;
connection.call("interfaceName", "methodName", new ArrayList<>(),
  new ExecutableHandler(executor, new OkErrorHandler() {
    @Override
    public void handleOk(List<?> data) {
      // ...
    }

    @Override
    public void handleError(Integer errorCode, List<?> data) {
      // ...
    }
  })
});
```

### JSTP compiler

JSTP compiler is a nice feature to ease handling of JSTP messages. You can
declare interfaces that correspond to specific API or simple call to avoid
writing all boilerplate code to get the arguments out of the message. JSTP
compiler will parse those at compile time and generate implementations.

#### Installation

[Check for the latest version](https://bintray.com/metarhia/maven/jstp-compiler)

Gradle:
```
dependencies {
  compile group: 'com.metarhia.jstp', name: 'jstp-compiler', version: '0.4.0'
}
```

Maven:

```
<dependency>
  <groupId>com.metarhia.jstp</groupId>
  <artifactId>jstp-compiler</artifactId>
  <version>0.4.0</version>
  <type>pom</type>
</dependency>
```

## Be aware that compiler documentation is currently outdated and needs a refresh as some methods/usages have changed.

#### Handlers usage

JSTP handlers are used to process data from incoming JSTP messages, just like
usual `ManualHandler`s do.  Unlike `ManualHandler`, you are able to customize
JSTP handlers as you wish, declaring methods with annotations described below.
To create your own handler, just add the  `@Handler` annotation to the
required interface.

```java
@Handler
public interface ExampleHandler {
  // ...
}
```

You also can make your handler extended from `ManualHandler` or
`ExecutableHandler` setting base class as an annotation parameter. For example,
if you want the messages to be handled on a separate thread, you can set
`ExecutableHandler.class` as a `@Handler` parameter:

```java
@Handler(ExecutableHandler.class)
public interface ExampleHandler {
  // ...
}
```

With this annotation the generated class will respect `ExecutableHandler`
interface and generate its code in `run()` method instead of `handle()` method.

##### @NotNull

Annotates that the method should only be called if all of its arguments are not
null (in case of method-wide annotation) or only specific parameters are not
null (in case of argument-wide annotations)

```java
@Handler
public interface ExampleHandler {
  // ...
  @NotNull
  void onExampleValue(List<?> args);
  // ...
}
```

##### @Object

Gets the field of the received message by specified name. It also allows getting
elements from nested objects, the value will be retrieved in the order of keys
specified.

```java
@Handler
public interface OkErrorHandler {
  // ...
  @NotNull
  @Object("ok")
  void onOK(List<?> args);

  @NotNull
  @Object("error")
  void onError(List<?> args);

  // gets String value by key "neededValue" in object got by "ok"
  @Object({"ok", "neededValue"})
  void onNeededValueRetrieved(String value);
  // ...
}
```

##### @Array

Can be used to get the specific value from JSTP message. It also allows
getting elements from nested arrays, the value will be retrieved in the order
of indexes specified.

```java
@Handler
public interface ExampleHandler {
  // ...
  void onFirstIndex(@Array(1) String arg);

  // gets (List<?>) message[1][2]
   void onValueBySecondIndex(@Array({1, 2}) List<?> args);
  // ...
}
```

##### @Mixed

It is a sort of combination of `@Object` and `@Array` annotations. You can get
needed value by index or by key. It also allows getting elements from nested
objects and arrays, the value will be retrieved in the order of keys and
indexes specified. To get a value by key, you should just declare the required
key like in `@Object` annotation, for example `"some key"`. To get value from
array by index, you can declare it as `"[index]"`. To get an object value by
index (according to keys order) you should declare it as `"{key index}"`.

```java
@Handler
public interface ExampleHandler {
  // ...

  @Mixed("ok")
  void onNeededNamedValue(Object args);

  @Mixed("{1}")
  void onKeyByIndexValue(Object args);

  // gets message["ok"][1][2]
  @Mixed({"ok", "[1]", "{2}"})
  void onNeededMixValue(Object args);
  // ...
}
```

You can use `@Array`, `@Object`, `@Mixed` annotations with both
methods and parameters. In case of method it'll decide starting
value to get from (to apply getters) for other parameter-wide
getters. If no method-wide getter is specified the value
under second key of object'll be used by default, to cancel
this behaviour you can either define you own getter or specify
`@NoDefaultGet` annotation (this way starting value'll be
the jstp message itself).

After compilation class named like `JSTP + (YourHandlerName)` (for this example
it will be `JSTPExampleHandler`) will be generated and you will be able to use
it in message processing.

```java
connection.call("interfaceName", "methodName", args, new ExampleHandler() {
    // ...
});
```
#### JSTP Receivers
You can process received values not only via single handler, but by several
ones. They can be added or removed from `Receiver` via `addHandler()` and
`removeHandler()` methods. JSTP receiver is generated similarly to `Handler`.
To generate receiver, you need to do the following:

```java
@Receiver
public interface ExampleReceiver {
  // ...
}
```
The syntax of declaring methods is the same as in `Handler`. After
compilation class named like `JSTP + (Your receiver name)` (for this example it
will be `JSTPExampleReceiver`) will be generated and you will be able to use it
in message processing.

You can use custom receiver like this:
```java

JSTPExampleReceiver receiver = new JSTPExampleReceiver();
receiver.addHandler(new ExampleReceiver() {
    // ...
});
receiver.addHandler(new ExampleReceiver() {
    // ...
});
connection.call("interfaceName", "methodName", args, receiver);
```

#### @Proxy

`@Proxy` annotation allows you to easily auto-generate interface to the remote
server that will look as if you are calling local java methods. To create
 a proxy, you just need to describe the interface with appropriate annotations.

To declare the proxy interface, you need to add the `@Proxy` annotation to the
interface definition. If you want your proxy to be a singleton, set the
`singleton` parameter of the annotation to `true`. If you want to set the
default interface name for the `Connection` methods, you can set it with
`interfaceName` parameter.

```java
// Creates singleton proxy with default interface name "defaultInterface"
@Proxy(interfaceName = "defaultInterface", singleton = true)
public interface MyProxy {
  // proxy methods
}
```

For now, you can declare proxy wrappers for sending `call` and `event` packets.

##### Call

To create wrapper for `call` method, use `@Call` annotation. Set the method
name as an annotation parameter (the default interface will be used), or
interface name and method name. The method name is not required as well: if you
use `@Call` annotation without parameters at all, your Java method name will be
used as a `call` method name. Specify the arguments and callback handler
(if they are needed) as your method parameters. See the examples:

```java

@Proxy(interfaceName = "defaultInterface")
public interface MyProxy {
  // Makes a call with specified "interfaceName" and "methodName", sets "param"
  // and "otherParam" as call arguments and uses "hander" to handle method
  // callback
  @Call({"interfaceName", "methodName"})
  void callInterfaceMethod(String param, int otherParam, ManualHandler handler);

  // Makes a call with default interface name and method name "methodName"
  // without arguments and uses "hander" to handle method callback
  @Call("methodName")
  void callDefaultInterfaceMethod(ManualHandler handler);

  // Makes a call with default interface name and method name "otherMethodName"
  // without arguments and without callback handler
  @Call("otherMethodName")
  void callNoParametersMethod();

  // Makes a call with default interface name and method name joinChat
  // without arguments and uses handler to handle method callback
  @Call()
  void joinChat(ManualHandler handler);
}
```

##### Event

To create wrapper for `event` method, use `@Event` annotation. Set the method
name as an annotation parameter (the default interface will be used), or
interface name and method name. The event name is not required as well: if you
use `@Event` annotation without parameters at all, your Java method name will be
used as an `event` name. Specify the arguments (if they are needed) as
your method parameters. See the examples:

```java
@Proxy(interfaceName = "defaultInterface")
public interface MyProxy {
  // Sends event with specified interface name "interfaceName" and event name
  // "eventName" without parameters
  @Event({"interfaceName", "eventName"})
  void sendInterfaceEvent();

  // Sends event with default interface name and event name "eventName" with
  // specified parameters
  @Event("eventName")
  void sendEvent(String param);

  // Sends event with default interface name and event name
  // onSomethingHappened without parameters
  @Event()
  void onSomethingHappened();
}
```

After compilation class named like `JSTP + (Your proxy name)` (for this example
it will be `JSTPMyProxy`) will be generated. See some usage examples:

```java
Connection connection;
Executor executor;
// ...
JSTPMyProxy proxy = new JSTPMyProxy(connection);
// calls method and handles callback by manual handler
proxy.callDefaultInterfaceMethod(new ManualHandler() {
    @Override
    public void handle(JSObject jsObject) {
        // handle result
    }
});

// calls method and handles callback by handler created with @Handler annotation
// (see the example of generating handlers)
proxy.callDefaultInterfaceMethod(new JSTPOkErrorHandler() {
    @Override
    public void onOk(List<?> args) {
        // handle data
    }

    @Override
    public void onError(Integer errorCode) {
        // handle error code
    }
});

// calls method and handles callback by executable handler (see the example of
// executable handler)
proxy.callDefaultInterfaceMethod(new ExecutableHandler(executor) {
    @Override
    public void run() {
        // handle message received
    }
});

// sends event with specified parameters
proxy.sendEvent("myParam");
```


##### Call handler

You can process incoming `call` packets by setting `call` handlers for called
methods, just like if you did it with `Connection` directly. You can set `call`
handler by `setCallHandler()` method, and remove your handler by
`removeCallHandler()` method. See the examples:

```java
// sets manual call handler for "methodName" of "interfaceName"
proxy.setCallHandler("interfaceName", "methodName", new ManualHandler() {
    @Override
    public void handle(JSObject jsObject) {
        // handle object
    }
});

// sets executable call handler for "methodName" of "interfaceName" (see the
// example of executable handler)
proxy.setCallHandler("interfaceName", "methodName",
        new ExecutableHandler(executor) {
    @Override
    public void run() {
        // handle message received
    }
});
```

If we want to use handler created with `@Handler` annotation like this

```java
@Handler
public interface OkErrorHandler {

    @NotNull
    @Object("ok")
    void onOk(List<?> args);

    @NotNull
    @Object("error")
    void onError(@Array(0) Integer errorCode);
}
```

We can use generated `JSTPOkErrorHandler` as a call handler for proxy:

```java
proxy.setCallHandler("interfaceName", "methodName", new JSTPOkErrorHandler() {
    @Override
    public void onOk(List<?> args) {
        // handle data
    }

    @Override
    public void onError(Integer errorCode) {
        // handle error code
    }
});
```

##### Event handler

You can process incoming `event` packets by adding `event` handlers, just like
if you did it with `Connection` directly. You can add `event` handler by
`addEventHandler()` method, and remove your handler by `removeEventHandler()`
method. See the examples:

```java
// adds manual event handler for "eventName" of "interfaceName"
proxy.addEventHandler("interfaceName", "eventName", new ManualHandler() {
    @Override
    public void handle(JSObject jsObject) {
        // handle object
    }
});

// adds executable handler for "eventName" of "interfaceName" (see the example
// of executable handler)
proxy.addEventHandler("interfaceName", "eventName",
        new ExecutableHandler(executor) {
    @Override
    public void run() {
        // handle message received
    }
});
```

If we want to use handler created with `@Handler` annotation like this

```java
@Handler
public interface EventHandler {

    @Object("onMessage")
    void onMessage(@Array(0) String receivedMessage);
}
```

We can use generated `JSTPEventHandler` as an event handler for proxy:
```java
proxy.addEventHandler("interfaceName", "eventName", new JSTPEventHandler() {
    @Override
    public void onMessage(String receivedMessage) {
        // handle message
    }
});
```

If we modify the annotation in such a way:
```java
@Handler(ExecutableHandler.class)
public interface EventHandler {

    @Object("onMessage")
    void onMessage(@Array(0) String receivedMessage);
}
```

We can use handler with preferred executor:
```java
Executor executor;

// ...

proxy.addEventHandler("interfaceName", "eventName",
        new JSTPEventHandler(executor) {
    @Override
    public void onMessage(String receivedMessage) {
        // handle message
    }
});
```
