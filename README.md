# Java JSTP implementation

[JSTP documentation](https://github.com/metarhia/JSTP)

[Check for the latest JSTP version](https://bintray.com/metarhia/maven/jstp)

## Installation
Gradle:

Add this to your build.gradle (check for the latest version):
```
dependencies {
  compile group: 'com.metarhia.jstp', name: 'jstp', version: '0.7.0'
}
```

Maven:
```
<dependency>
  <groupId>com.metarhia.jstp</groupId>
  <artifactId>jstp</artifactId>
  <version>0.7.0</version>
  <type>pom</type>
</dependency>
```

## Parser usage

There are 2 parsers available:
1) To native java objects with `JSNativeParser`

Native parser gives you java Objects directly, which is more convenient.

Few simple examples
```java
Map<String, Number> a = (Map<String, Number>) new JSNativeParser("{a: 3}").parse();
a.get("a"); // returns 3.0

List<Number> arr = (List<Number>) new JSNativeParser("[1, 2, 3]").parse();
arr.get(1); // returns 2.0
```

To serialize objects you can use `JSNativeSerializer`
```java
List<Number> arr = (List<Number>) new JSNativeParser("[1, 2, 3]").parse();
JSNativeSerializer.stringify(arr); // returns "[1,2,3]"
```
If it doesn't know how to serialize input it'll be serialized as `undefined`

2) To simple js mirrored hierarchy in java with `JSParser` (hierarchy has
  `JSValue` as superclass)

In current JSTP SDK implementation, you can use:
* JSObject
* JSArray
* JSNumber
* JSString
* JSBool
* JSNull
* JSUndefined

To parse values, you can use JSParser directly:

```java
try {
  JSParser parser = new JSParser();
  JSValue value = parser.parse("ANY JS VALUE");
  JSObject obj = parser.parseJSObject("YOUR OBJECT VALUE");
  JSArray array = parser.parseJSArray("YOUR ARRAY VALUE");
} catch (JSParsingException e) {
  // error handling goes here
}
```

You also can use it like that:
```java
try {
  JSValue value = JSTP.parse("YOUR OBJECT VALUE");
} catch (JSParsingException e) {
  //...
}
```

Get field 'a' of object `{a: 3}`;
```java
try {
  JSObject obj = JSTP.parse("{a : 3}");
  obj.get("a"); // returns 3
} catch (JSParsingException e) {
  // error handling goes here
}
```

Get second element of array `[1, 2, 3]`;
```java
try {
  JSArray arr = (JSArray) new JSParser("[1, 2, 3]").parse();
  arr.get(1); // returns 2
} catch (JSParsingException e) {
  // error handling goes here
}
```

To convert values from js java hierarchy to js use `.toString()` method or
`JSTP.stringify`.
They can be parsed in js with a simple eval statement or
[js parser](https://github.com/metarhia/JSTP)
```java
JSValue value;
//...
String serializedValue = JSTP.stringify(value);
```

## JSTPConnection

### Establish connection

To establish JSTP connection, you need to provide transport.
As of now the only available transport is TCP. Optionally you can
define restoration policy (there are 2 basic ones in SDK:
`DropRestorationPolicy` - which will create new connection every
time transport is restored and `SessionRestorationPolicy` - which
will resend cached packets and try to restore session.
`SessionRestorationPolicy` is used by default). For example:

```java
String host = "metarhia.com";
int port = 80;
boolean usesSSL = true;

AbstractSocket transport = new TCPTransport(host, port, usesSSL);
JSTPConnection connection = new JSTPConnection(transport);
```

You can change used transport by calling `useTransport()` method.
This will close previous transport if available and set provided one
as current transport. It will try to connect and upon connection
appropriate method of restoration policy will be called when transport
reports that it's connected.

To react to connection events, you can use `JSTPConnectionListener`:

```java
connection.addSocketListener(new JSTPConnectionListener() {
  @Override
  public void onConnected(boolean restored) {
    // ...
  }

  @Override
  public void onPacketRejected(JSObject packet) {
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

### JSTP packet types

#### Handshake

Usually you don't have to send handshake packets manually. You may need
them if If you need to implement your own restoration policy or change
transport on active connection. You can send `handshake` packet as follows:

```java
// anonymous handshake message
connection.handshake("applicationName", new ManualHandler() {
  @Override
  public void invoke(JSValue packet) {
    // ...
  }
});

// handshake with attempt to restore session
connection.handshake("applicationName", "sessionId", new ManualHandler() {
  @Override
  public void invoke(JSValue packet) {
    // ...
  }
});

// handshake message with authorization
connection.handshake("applicationName", "username", "password", new ManualHandler() {
  @Override
  public void invoke(JSValue packet) {
    // ...
  }
});

```

#### Call

To send `call` message:

```java
JSArray args = new JSArray();
// ...
connection.call("interfaceName", "methodName", args, new ManualHandler() {
  @Override
  public void invoke(final JSValue value) {
    // ...
  }
);
```

To handle incoming `call` packets, you have to `setCallHandler()` for that call.
There can only be one call handler for each call.

#### Callback

While sending callback you should specify callback type (`JSCallback.OK` or
`JSCallback.ERROR`) and arbitrary arguments.

```java
connection.setCallHandler("interfaceName", "methodName", new CallHandler() {
  @Override
  public void handleCallback(JSArray data) {
    JSArray args = new JSArray();
    // ...
    callback(connection, JSCallback.OK, args);
  }
});
```

You also can send `callback` packets like this:

```java
connection.callback(JSCallback.OK, args);

// define custom packet index
Long customIndex;
// ...
connection.callback(JSCallback.OK, args, customIndex);

```

#### Inspect

Incoming inspect packets are handled by JSTPConnection itself. To make
methods visible through inspect message you just need to define method
names with appropriate interfaces.

```java
connection.setClientMethodNames("interfaceName1", "methodName1", "methodName2");
connection.setClientMethodNames("interfaceName2", "methodName1", "methodName2");
// ...
```

To send `inspect` packet:
```java
connection.inspect("interfaceName", new ManualHandler() {
  @Override
  public void invoke(JSValue packet) {
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
  public void invoke(JSValue packet) {
    // ...
  }
});
```

Sending `event` packet:
```java
JSArray args = new JSArray();
// ...
connection.event("interfaceName", "methodName", args);
```

### JSTP compiler

JSTP compiler is a nice feature to ease handling of JSTP packets. You can
declare interfaces that correspond to specific API or simple call to avoid
writing all boilerplate code to get the arguments out of the packet. JSTP
compiler will parse those at compile time and generate implementations.

#### Installation

[Check for the latest version](https://bintray.com/metarhia/maven/jstp-compiler)

Gradle:
```
dependencies {
  compile group: 'com.metarhia.jstp', name: 'jstp-compiler', version: '0.1.12'
}
```

Maven:

```
<dependency>
  <groupId>com.metarhia.jstp</groupId>
  <artifactId>jstp-compiler</artifactId>
  <version>0.1.12</version>
  <type>pom</type>
</dependency>
```

#### Handlers usage

JSTP handlers are used to process data from incoming JSTP packets, just like
usual `ManualHandler`s do.  Unlike `ManualHandler`, you are able to customize
JSTP handlers as you wish, declaring methods with annotations described below.
To create your own handler, just add the  `@JSTPHandler` annotation to the
required interface.

```java
@JSTPHandler
public interface ExampleHandler {
  // ...
}
```

##### NotNull

Annotates that the method should only be called if all of its arguments are not
null (in case of method-wide annotation) or only specific parameters are not
null (in case of argument-wide annotations)

```java
@JSTPHandler
public interface ExampleHandler {
  // ...
  @NotNull
  void onExampleValue(JSArray args);
  // ...
}
```

##### Named

Gets the field of the received packet by specified name. It also allows getting
elements from nested objects, the value will be retrieved in the order the keys
specified.

```java
@JSTPHandler
public interface OkErrorHandler {
  // ...
  @NotNull
  @Named("ok")
  void onOK(JSArray args);

  @NotNull
  @Named("error")
  void onError(JSArray args);

  // gets value by key "neededValue" in object got by "ok"
  @Named(value = {"ok", "neededValue"})
  void onNeededValueRetrieved(String value);
  // ...
}
```

##### Indexed

Can be used to get the specific value from JSTP message. It also allows
getting elements from nested arrays, the value will be retrieved in the order the
indexes specified.

```java
@JSTPHandler
public interface ExampleHandler {
  // ...
  void onFirstIndex(@Indexed(1) JSString arg);

  // gets packet[1][2]
   void onValueBySecondIndex(@Indexed(value = {1,2}) JSArray args);
  // ...
}
```

##### Custom-named

It is a sort of combination of `Named` and `Indexed` annotations. You can get
needed value by index or by key. It also allows getting elements from nested
objects and arrays, the value will be retrieved in the order the keys and indexes
 specified. To get a value by key, you should just declare the required key
like in `@Named` annotation, for example `"some key"`. To get value from array
by index, you can declare it as `"[index]"`. To get an object value by index
(according to keys order) you should declare it as `"{key index}"`.

```java
@JSTPHandler
public interface ExampleHandler {
  // ...

  @CustomNamed("ok")
  void onNeededNamedValue(JSValue args);

  @CustomNamed("{1}")
  void onKeyByIndexValue(JSValue args);

  // gets packet["ok"][1][2]
  @CustomNamed(value = {"ok", "[1]", "{2}"})
  void onNeededMixValue(JSValue args);
  // ...
}
```

After compilation class named like `JSTP + (YourHandlerName)` (for this example
it will be `JSTPExampleHandler`) will be generated and you will be able to use
it in packet processing.

```java
connection.call("interfaceName", "methodName", args, new JSTPExampleHandler() {
    // ...
});
```
#### JSTP receiver
You can process received values not only via single handler, but by several
ones. They can be added or removed from `JSTP receiver` via `addHandler()` and
`removeHandler() methods. JSTP receiver is generated similarly to `JSTPHandler`.
To generate receiver, you need to do the following:

```java
@JSTPReceiver
public interface ExampleReceiver {
  // ...
}
```
The syntax of declaring methods is the same as in `JSTPHandler`. After
compilation class named like `JSTP + (Your receiver name)` (for this example it
will be `JSTPExampleReceiver`) will be generated and you will be able to use it
in packet processing.

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
