##Java JSTP parser

##Usage example
```java
try {
  JSParser parser = new JSParser();
  JSValue value = parser.parse("JSTP PACKET");
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

JSValues are:
* JSObject
* JSArray
* JSNumber (saved as double)
* JSString
* JSBool
* JSNull
* JSUndefined

##JSTPConnection

Use connection like this:
```java
String host = "your host";
int port = 80;
JSTPConnection connection = new JSTPConnection(host, port);
connection.handshake("applicationName");

JSArray args = new JSArray();
args.put("some args");
connection.call("interfaceName", "methodName", args, new JSTPConnection.ResponseHandler() {
    @Override
    public void onResponse(final JSValue value) {
        //do smth with received response
    }
}
```
