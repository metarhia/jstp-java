# Java JSTP implementation

[Read here about JSTP](https://github.com/metarhia/JSTP)

[Check for the latest JSTP version](https://bintray.com/metarhia/maven/jstp)

## Add JSTP SDK to your project
Gradle:

Add this to your build.gradle (check for the latest version):
```
dependencies {
  compile 'com.metarhia.jstp:jstp:0.7.0'
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

## JSTP parser

All javascript data is parsed to `JSValue`.

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
  //...
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

```java
JSValue value;
//...
String deserializedValue = JSTP.stringify(value);
```

## JSTPConnection

### Establish connection

To establish JSTP connection, you need to define preferred transport and restoration policy (you can implement your own restoration policy or use one of restoration policies offered by SDK: `DropRestorationPolicy` or `SessionRestorationPolicy`. `SessionRestorationPolicy` is used by default). For example:

```java
String host = "metarhia.com";
int port = 80;
boolean usesSSL = true;

AbstractSocket transport = new TCPTransport(host, port, usesSSL);
JSTPConnection connection = new JSTPConnection(transport);
```

You can change used transport by calling `useTransport()` method.
If you implement your own restoration policy, you need to send `handshake` packet on your own.

To process connection events, add `JSTPConnectionListener` to your connection.

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

You can define applicationName and/or session Id when connecting, or connect without them:
```java
connection.connect();
// ...
connection.connect("applicationName");
// ...
connection.connect("applicationName", "sessionId");
```

### JSTP packet types

#### Handshake

You don't have to send handshake packets manually using SDK restoration policies. If you need to send handshake manually, you can do it in the following ways:

```java

// for anonymous handshake message
connection.handshake("applicationName", new ManualHandler() {
  @Override
  public void invoke(JSValue packet) {
    // ...
  }
});

// for handshake with attempt to restore session

connection.handshake("applicationName", "sessionId", new ManualHandler() {
  @Override
  public void invoke(JSValue packet) {
    // ...
  }
});

// for handshake message with authorization

connection.handshake("applicationName", "username", "password", new ManualHandler() {
  @Override
  public void invoke(JSValue packet) {
    // ...
  }
});

```

#### Call

Sending `call` message looks like as follows:

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

#### Callback

To handle incoming `call` packets, you have to `setCallHandler()` to your connection. You should specify callback type (`JSCallback.OK` or `JSCallback.ERROR`) and arguments.

```java
connection.setCallHandler("methodName", new CallHandler() {
  @Override
  public void handleCallback(JSArray data) {
    JSArray args = new JSArray();
    // ...
    callback(connection, JSCallback.OK, args));
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

Incoming inspect packets are handled by JSTPConnection itself. You just need to define client method names in your connection.

```java
connection.setClientMethodNames("interfaceName1", "methodName1", "methodName2");
connection.setClientMethodNames("interfaceName2", "methodName1", "methodName2");
// ...
```

Sending `inspect` packet looks like this:
```java
connection.inspect("interfaceName", new ManualHandler() {
  @Override
  public void invoke(JSValue packet) {
    // ...
  }
});
```

#### Event

Events are handled very alike to callbacks. To handle incoming events, add event handlers to your connection.

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
