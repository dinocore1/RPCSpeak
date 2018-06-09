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

    private int mSendIdx = 0;

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
        int diff = seqNum - mN_a;
        if(diff > 0) {
            mOutputBuffer.skip(diff);
            mN_a += diff;
            mN_a = BasicStreamingProtocol.normializeSequenceNum(mN_a);
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
                size = Math.min(size, available - mSendIdx);
                size = mOutputBuffer.peek(mTempBuffer, BasicStreamingProtocol.HEADER_SIZE, size, mSendIdx);
                BasicStreamingProtocol.writeHeader(mTempBuffer, 0, mN_a + mSendIdx, false);
                mSocket.send(mTempBuffer, 0, size + BasicStreamingProtocol.HEADER_SIZE);
                mSendIdx = (mSendIdx + size) % available;

            }
        }

    }


    @Override
    public void flush() throws IOException {

    }
}
