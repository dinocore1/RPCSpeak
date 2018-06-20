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

    public byte[] mTempBuffer;

    // w_t
    public final int mWindowSize;
    private final DatagramSocket mSocket;
    private CircleByteBuffer mOutputBuffer;

    //Highest acknowledged sequence num (n_a)
    private int mN_a = 0;

    public SlidingWindowOutputStream(int windowSize, DatagramSocket socket) {
        this.mWindowSize = windowSize;
        this.mSocket = socket;
        mOutputBuffer = new CircleByteBuffer(windowSize);
        mTempBuffer = new byte[mWindowSize + BasicStreamingProtocol.HEADER_SIZE];
    }

    public SlidingWindowOutputStream(DatagramSocket socket) {
        this(1024, socket);
    }


    synchronized void ackReceived(int seqNum) {

        if(mN_a <= seqNum) {
            int diff = seqNum - mN_a;
            mOutputBuffer.skip(diff);
            mN_a = seqNum;
        } else {
            int diff = BasicStreamingProtocol.MAX_SEQUENCE_NUM - mN_a;
            diff += seqNum;
            mOutputBuffer.skip(diff);
            mN_a = seqNum;
        }



        notifyAll();
    }

    @Override
    public synchronized void write(int i) throws IOException {

        while(!mOutputBuffer.put((byte) i)) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                LOGGER.warn("", e);
            }
        }

    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        int bytesWritten = 0;
        while(len > 0) {
            int count = mOutputBuffer.put(b, off + bytesWritten, len);
            bytesWritten += count;
            len -= count;


            push();

            if(count == 0) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    LOGGER.warn("", e);
                }
            }

        }
    }

    void push() {

        synchronized (this) {

            int bytesWritten = 0;
            int available = mOutputBuffer.getAvailable();
            available = Math.min(available, mTempBuffer.length - BasicStreamingProtocol.HEADER_SIZE);

            while(available > 0) {
                int count = mOutputBuffer.peek(mTempBuffer, BasicStreamingProtocol.HEADER_SIZE + bytesWritten, available, bytesWritten);
                bytesWritten += count;
                available -= count;
            }

            if(bytesWritten > 0) {
                BasicStreamingProtocol.writeHeader(mTempBuffer, 0, mN_a, false);
                mSocket.send(mTempBuffer, 0, BasicStreamingProtocol.HEADER_SIZE + bytesWritten);
            }

        }

    }


    @Override
    public void flush() throws IOException {

    }
}
