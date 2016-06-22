package com.devsmart.rpcspeak;


import com.devsmart.ubjson.UBArray;
import com.devsmart.ubjson.UBObject;

public class Request {

    final int mId;
    final String mMethod;
    final UBArray mArgs;

    public Request(int id, String method, UBArray args) {
        mId = id;
        mMethod = method;
        mArgs = args;
    }

    public Request(UBObject obj) {
        this(obj.get(RPCEndpoint.KEY_ID).asInt(),
                obj.get(RPCEndpoint.KEY_METHOD).asString(),
                obj.get(RPCEndpoint.KEY_ARGS).asArray());
    }


}
