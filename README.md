# Java JSTP implementation

[JSTP documentation](https://github.com/metarhia/JSTP)

[Check for the latest JSTP version](https://bintray.com/metarhia/maven/jstp)

## Installation
Gradle:

Add this to your build.gradle (check for the latest version):
```
dependencies {
  compile group: 'com.metarhia.jstp', name: 'jstp', version: '0.8.0'
}
```

Maven:
```
<dependency>
  <groupId>com.metarhia.jstp</groupId>
  <artifactId>jstp</artifactId>
  <version>0.8.0</version>
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
a.get("a"); // returns 3.0

// Get second field of object {a: 3, b: 2}
a.getByIndex(1); // returns 2.0

// But you can easily use it as a Map
Map<String, Number> map = (Map<String, Number>) a;
// or
Map<String, Number> a = JSParser.parse("{a: 3, b: 2}");


List<Double> arr = JSParser.parse("[1, 2, 3]");
// Get second element of array [1, 2, 3];
arr.get(1); // returns 2.0


String str = JSParser.parse("'abc'");
// str now equals to 'abc'


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

If it doesn't know how to serialize input it'll be serialized as `undefined`,
also you can define how your objects will be serialized via `JSSerializable`
interface.

They can be parsed in js with a simple eval statement or
[js parser](https://github.com/metarhia/JSTP)

## Connection

### Establish connection

To establish JSTP connection, you need to provide transport.
As of now the only available transport is TCP. Optionally you can
define restoration policy (there are 2 basic ones in SDK:
`DropRestorationPolicy` - which will create new connection every
time transport is restored and `SessionRestorationPolicy` - which
will resend cached messages and try to restore session.
`SessionRestorationPolicy` is used by default). For example:

```java
String host = "metarhia.com";
int port = 80;
boolean usesSSL = true;

AbstractSocket transport = new TCPTransport(host, port, usesSSL);
Connection connection = new Connection(transport);
```

You can change used transport by calling `useTransport()` method.
This will close previous transport if available and set provided one
as current transport. It will try to connect and upon connection
appropriate method of restoration policy will be called when transport
reports that it's connected.

To react to connection events, you can use `ConnectionListener`:

```java
connection.addSocketListener(new ConnectionListener() {
  @Override
  public void onConnected(boolean restored) {
    // ...
  }

  @Override
  public void onMessageRejected(JSObject message) {
    // ...
  }

  @Override
  public void onConnectionError(int errorCode) {
    // ...
  }

  @Override
  public void onConnectionClosed() {
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
them if If you need to implement your own restoration policy or change
transport on active connection. You can send `handshake` message as follows:

```java
// anonymous handshake message
connection.handshake("applicationName", new ManualHandler() {
  @Override
  public void handle(JSObject message) {
    // ...
  }
});

// handshake with attempt to restore session
connection.handshake("applicationName", "sessionId", new ManualHandler() {
  @Override
  public void handle(JSObject message) {
    // ...
  }
});

// handshake message with authorization
connection.handshake("applicationName", "name", "pass", new ManualHandler() {
  @Override
  public void handle(JSObject  message) {
    // ...
  }
});

```

#### Call

To send `call` message:

```java
List<?> args = new ArrayList();
// ...
connection.call("interfaceName", "methodName", args, new ManualHandler() {
  @Override
  public void handle(final JSObject message) {
    // ...
  }
);
```

To handle incoming `call` messages, you have to `setCallHandler()` for that call.
There can only be one call handler for each call.

#### Callback

While sending callback you should specify callback type (`JSCallback.OK` or
`JSCallback.ERROR`) and arbitrary arguments.

```java
connection.setCallHandler("interfaceName", "methodName", new CallHandler() {
  @Override
  public void handleCallback(List<?> data) {
    List<Object> args = new ArrayList<>();
    // ...
    callback(connection, JSCallback.OK, args);
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

Incoming inspect messages are handled by Connection itself. To make
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
  public void handle(JSObject message) {
    // ...
  }
});
```

#### Event

To handle incoming events, you add event handlers with `addEventHandler()`.
There can be multiple event handlers for each event.

```java
connection.addEventHandler("interfaceName", "methodName", new ManualHandler() {
  @Override
  public void handle(JSObject message) {
    // ...
  }
});
```

Sending `event` message:
```java
List<Object> args = new ArrayList<>();
// ...
connection.event("interfaceName", "methodName", args);
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
  compile group: 'com.metarhia.jstp', name: 'jstp-compiler', version: '0.2.0'
}
```

Maven:

```
<dependency>
  <groupId>com.metarhia.jstp</groupId>
  <artifactId>jstp-compiler</artifactId>
  <version>0.2.0</version>
  <type>pom</type>
</dependency>
```

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
`removeHandler() methods. JSTP receiver is generated similarly to `Handler`.
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
