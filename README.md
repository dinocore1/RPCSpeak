## RPCSpeak ##

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.dev-smart/microdb-runtime/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.dev-smart/rpcspeak)

RPCSpeak is a pure-java library for performing Remote Procedure Calls (RPC).
RPCSpeak's protocol is simular to [JSON-RPC](http://json-rpc.org) except its uses the more efficient
binary [UBJSON](http://ubjson.org/) format for serializing data.

Setting up a RPC service is very simple. All you need is a generic InputStream
and OutputStream.

#### Client Side ####

```
// connect the client
RCPEndpoint endpoint = new RPCEndpoint(inputStream, outputStream);
endpoint.start();


//create request message
UBArray args = UBValueFactor.createArray();
UBValue response = endpoint.RPC("hello", args);

// response now has the response from the remote service

```

#### Server Side ####

```
// connect the service
RCPEndpoint endpoint = new RPCEndpoint(inputStream, outputStream);

endpoint.registerMethod("hello", new RPC() {
    @Override
    public UBValue invoke(UBArray args) {
        return UBValueFactory.createString("hi");
    }
});

endpoint.start();

```