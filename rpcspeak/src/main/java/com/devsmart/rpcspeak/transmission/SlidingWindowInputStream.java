package com.devsmart.rpcspeak.transmission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class SlidingWindowInputStream extends InputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlidingWindowInputStream.class);

    /**
     * Receiver
     * This class implements Go-Back-N sliding window protocol
     * This implementation assumes a fixed size MTU
     */

    private static final int WINDOW_SIZE = 1;

    public final int bufferSize;
    private final DatagramSocket mSocket;


    //Current sequence number (n_r) next packet to be received
    int mN_r = 0;


    private CircleByteBuffer mDataBuffer;
    private byte[] mTmpBuffer = new byte[BasicStreamingProtocol.HEADER_SIZE];

    public SlidingWindowInputStream(int bufferSize, DatagramSocket socket) {
        this.bufferSize = bufferSize;
        mSocket = socket;
        mDataBuffer = new CircleByteBuffer(bufferSize);
    }

    public SlidingWindowInputStream(DatagramSocket socket) {
        this(1024, socket);
    }

    synchronized void packetReceived(int seqNum, byte[] buffer, int bufferSize) {

        int free = mDataBuffer.free();

        for(int i=0;i<bufferSize-BasicStreamingProtocol.HEADER_SIZE;i++) {

            if(free > 0 && mN_r == (seqNum + i) % BasicStreamingProtocol.MAX_SEQUENCE_NUM) {

                mDataBuffer.put(buffer[BasicStreamingProtocol.HEADER_SIZE + i]);
                mN_r = (mN_r + 1) % BasicStreamingProtocol.MAX_SEQUENCE_NUM;
                free--;

            }
        }

        notifyAll();

        //send ack packet
        BasicStreamingProtocol.writeHeader(mTmpBuffer, 0, mN_r, true);
        mSocket.send(mTmpBuffer, 0, BasicStreamingProtocol.HEADER_SIZE);

    }

    void push() {
        synchronized (this) {
        }
    }


    @Override
    synchronized public int read() throws IOException {

        while(mDataBuffer.getAvailable() == 0) {
            try {
                wait(100);
            } catch (InterruptedException e) {
                LOGGER.warn("", e);
            }
        }

        return (0xFF & mDataBuffer.get());
    }


}
