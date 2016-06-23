package com.devsmart.rpcspeak;


import com.devsmart.ubjson.UBArray;
import com.devsmart.ubjson.UBObject;
import com.devsmart.ubjson.UBValue;
import com.devsmart.ubjson.UBValueFactory;

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


    public UBValue toMsg() {
        UBObject retval = UBValueFactory.createObject();
        retval.put(RPCEndpoint.KEY_TYPE, UBValueFactory.createInt(RPCEndpoint.TYPE_REQUEST));
        retval.put(RPCEndpoint.KEY_ID, UBValueFactory.createInt(mId));
        retval.put(RPCEndpoint.KEY_METHOD, UBValueFactory.createString(mMethod));
        retval.put(RPCEndpoint.KEY_ARGS, mArgs);
        return retval;
    }
}
