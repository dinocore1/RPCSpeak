package com.devsmart.rpcspeak;


import com.devsmart.ubjson.UBArray;
import com.devsmart.ubjson.UBObject;

public class Request {

    final int mId;
    final String mMethod;
    final UBArray mArgs;

    public Request(UBObject obj) {
        mId = obj.get(RPCEndpoint.KEY_ID).asInt();
        mMethod = obj.get(RPCEndpoint.KEY_METHOD).asString();
        mArgs = obj.get(RPCEndpoint.KEY_ARGS).asArray();
    }
}
