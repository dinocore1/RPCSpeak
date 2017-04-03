package com.devsmart.rpcspeak;


import com.devsmart.ubjson.UBArray;
import com.devsmart.ubjson.UBValue;

public interface RPC {

    UBValue invoke(UBArray args);
}
