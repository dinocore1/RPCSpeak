package com.devsmart.rpcspeak.transmission;

import com.devsmart.rpcspeak.transmission.DatagramSocket;

import java.io.IOException;
import java.io.InputStream;

public class SlidingWindowInputStream extends InputStream {

    /**
     * Receiver
     * This class implements Go-Back-N sliding window protocol
     * This implementation assumes a fixed size MTU
     */

    public final int mtu;
    private final DatagramSocket mSocket;

    //Current sequence number (n_r) next packet to be received
    private int mSequenceNum;

    //Window size (w_t)
    private static final int WINDOW_SIZE = 1;

    public SlidingWindowInputStream(int mtu, DatagramSocket socket) {
        this.mtu = mtu;
        this.mSocket = socket;
    }

    public SlidingWindowInputStream(DatagramSocket socket) {
        this(1024, socket);
    }

    @Override
    public int read() throws IOException {
        return 0;
    }
}
