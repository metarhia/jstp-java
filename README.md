#Java JSTP parser

[Read more here](https://github.com/metarhia/JSTP)

##Add JSTP to your project
Gradle:
```
compile 'com.metarhia.jstp:jstp:0.2.1'
```

Maven:
```
<dependency>
  <groupId>com.metarhia.jstp</groupId>
  <artifactId>jstp</artifactId>
  <version>0.2.1</version>
  <type>pom</type>
</dependency>
```

##JSTP parser

All javascript data is parsed to JSValue.

JSValues are:
* JSObject
* JSArray
* JSNumber (saved as double)
* JSString
* JSBool
* JSNull
* JSUndefined

To parse js, you can use JSParser directly:

```java
try {
  JSParser parser = new JSParser();
  JSValue value = parser.parse("ANY JS VALUE");
  JSObject obj = parser.parseJSObject("YOUR OBJECT VALUE");
  JSArray array = parser.parseJSArray("YOUR ARRAY VALUE");
} catch(JSParsingException e) {
  //...
}
```
You also can use it like that:
```java
try {
  JSValue value = JSTP.parse("YOUR OBJECT VALUE");
} catch(JSParsingException e) {
  //...
}
```

```java
JSValue value;
//...
String deserializedValue = JSTP.stringify(value);
```

##JSTPConnection

Use connection like this:
```java
String host = "your host";
int port = 80;

JSTPConnection connection = new JSTPConnection(host, port);
//used to handle network and parsing errors
connection.setErrorListener(this);
//used for incoming inspect packages
connection.setClientMethodNames("myClientMethod1", "myClientMethod2");
connection.handshake("applicationName");
```

Call:

```java
JSArray args = new JSArray();
args.put("some args");
connection.call("chat", "signIn", authArgs, new ManualHandler() {
  @Override
  public void invoke(final JSValue value) {
    //do something with received value
  }
);
```
Handle event:

```java

connection.addEventHandler("eventName", new ManualHandler() {
  @Override
  public void invoke(final JSValue value) {
    //do something with received value
  }
);
```

Handle callback:
```java
connection.addCallHandler("methodName", new ManualHandler() {
  @Override
  public void invoke(final JSValue value) {
    //do something with received value
  }
);
```
