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


    //Window size (w_t)
    static final int WINDOW_SIZE = 1;

    public final int mtu;
    private final DatagramSocket mSocket;

    //Current sequence number (n_r) next packet to be received
    int mN_r = 0;


    private CircleByteBuffer mDataBuffer;
    private byte[] mTmpBuffer = new byte[BasicStreamingProtocol.HEADER_SIZE];

    public SlidingWindowInputStream(int mtu, DatagramSocket socket) {
        this.mtu = mtu;
        mSocket = socket;

        mDataBuffer = new CircleByteBuffer(mtu);
    }

    public SlidingWindowInputStream(DatagramSocket socket) {
        this(1024, socket);
    }

    synchronized void packetReceived(int seqNum, byte[] buffer, int bufferSize) {

        int free;
        int end = seqNum + bufferSize - BasicStreamingProtocol.HEADER_SIZE;

        if(seqNum <= mN_r && mN_r < end && (free = mDataBuffer.free()) > 0) {
            int size = Math.min(free, end - mN_r);
            int offset = mN_r - seqNum;
            int bytesWritten = mDataBuffer.put(buffer, offset + BasicStreamingProtocol.HEADER_SIZE, size);
            mN_r += bytesWritten;

            notifyAll();
        }

        //send ack packet
        BasicStreamingProtocol.writeHeader(mTmpBuffer, 0, mN_r, true);
        mSocket.send(mTmpBuffer, 0, BasicStreamingProtocol.HEADER_SIZE);

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

        return mDataBuffer.get();
    }


}
