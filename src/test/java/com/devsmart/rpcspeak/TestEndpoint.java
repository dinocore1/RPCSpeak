package com.devsmart.rpcspeak;


import com.devsmart.ubjson.UBArray;
import com.devsmart.ubjson.UBValue;
import com.devsmart.ubjson.UBValueFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class TestEndpoint {

    @Test
    public void testSendRequest() throws Exception {

        PipedInputStream ina = new PipedInputStream();
        PipedOutputStream outb = new PipedOutputStream(ina);

        PipedInputStream inb = new PipedInputStream();
        PipedOutputStream outa = new PipedOutputStream(inb);

        RPCEndpoint a = new RPCEndpoint(1, 2, ina, outa);
        RPCEndpoint b = new RPCEndpoint(1, 2, inb, outb);

        b.registerMethod("hello", new RPC() {
            @Override
            public UBValue invoke(UBArray args) {
                System.out.println(String.format("b invoke args: %s", args));
                return UBValueFactory.createNull();
            }
        });

        a.start();
        b.start();

        a.RPC("hello", UBValueFactory.createArray(
                UBValueFactory.createString("one"),
                UBValueFactory.createInt(2)));

        a.shutdown();
        b.shutdown();

    }
}
