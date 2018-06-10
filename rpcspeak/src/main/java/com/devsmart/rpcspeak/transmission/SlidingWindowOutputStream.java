package com.devsmart.rpcspeak.transmission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

public class SlidingWindowOutputStream extends OutputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlidingWindowOutputStream.class);

    /**
     * Transmitter
     * This implementation assumes a fixed size MTU
     */

    //Window size (w_t)
    private static final int WINDOW_SIZE = 1024;

    public byte[] mTempBuffer;

    public final int mtu;
    private final DatagramSocket mSocket;
    private CircleByteBuffer mOutputBuffer;

    //Highest acknolaged sequence num (n_a)
    private int mN_a = 0;
    private int mN_t;
    private int mTotal;

    public SlidingWindowOutputStream(int mtu, DatagramSocket socket) {
        this.mtu = mtu;
        this.mSocket = socket;
        mOutputBuffer = new CircleByteBuffer(WINDOW_SIZE);
        mTempBuffer = new byte[mtu];
    }

    public SlidingWindowOutputStream(DatagramSocket socket) {
        this(1024, socket);
    }


    synchronized void ackReceived(int seqNum) {

        int diff;
        if(seqNum < mN_a) {
            diff = seqNum;
            diff += BasicStreamingProtocol.MAX_SEQUENCE_NUM - mN_a;

        } else {
            diff = seqNum - mN_a;
        }

        if(diff > 0) {
            mTotal += diff;
            mOutputBuffer.skip(diff);
            mN_a = (mN_a + diff) % (BasicStreamingProtocol.MAX_SEQUENCE_NUM);
        }

        notifyAll();
    }

    @Override
    public synchronized void write(int i) throws IOException {

        while(!mOutputBuffer.put((byte) i)) {
            try {
                wait(100);
            } catch (InterruptedException e) {
                LOGGER.warn("", e);
            }
        }

    }

    void push() {

        synchronized (this) {
            int available = mOutputBuffer.getAvailable();

            if(available > 0) {

                int size = Math.min(available, mTempBuffer.length - BasicStreamingProtocol.HEADER_SIZE);
                size = mOutputBuffer.peek(mTempBuffer, BasicStreamingProtocol.HEADER_SIZE, size, 0);
                mN_t = mN_a + size;
                BasicStreamingProtocol.writeHeader(mTempBuffer, 0, mN_a, false);
                mSocket.send(mTempBuffer, 0, size + BasicStreamingProtocol.HEADER_SIZE);
                //mSendIdx = (mSendIdx + size) % WINDOW_SIZE;

            }
        }

    }


    @Override
    public void flush() throws IOException {

    }
}
