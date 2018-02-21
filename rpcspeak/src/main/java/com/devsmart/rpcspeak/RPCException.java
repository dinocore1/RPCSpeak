package com.devsmart.rpcspeak;

import com.devsmart.ubjson.UBValue;

public class RPCException extends Exception {

    private final UBValue mExceptObj;

    public RPCException(UBValue exceptObj) {
        mExceptObj = exceptObj;
    }
}
