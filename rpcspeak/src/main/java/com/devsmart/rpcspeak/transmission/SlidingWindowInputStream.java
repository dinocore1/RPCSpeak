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

    public final int mtu;
    private final DatagramSocket mSocket;

    //Current sequence number (n_r) next packet to be received
    int mSequenceNum = 1;

    //Window size (w_t)
    static final int WINDOW_SIZE = 1;

    private byte[] mBuffer;
    private int mBufferOffset;
    private int mPacketSize;

    public SlidingWindowInputStream(int mtu, DatagramSocket socket) {
        this.mtu = mtu;
        this.mSocket = socket;

        mBuffer = new byte[(mtu + BasicStreamingProtocol.HEADER_SIZE) * WINDOW_SIZE];
        mBufferOffset = BasicStreamingProtocol.HEADER_SIZE;
    }

    public SlidingWindowInputStream(DatagramSocket socket) {
        this(1024, socket);
    }

    synchronized void packetReceived(int seqNum, byte[] buffer, int bufferSize) {
        if(mSequenceNum == seqNum) {
            System.arraycopy(buffer, 0, mBuffer, 0, bufferSize);
            mPacketSize = bufferSize - BasicStreamingProtocol.HEADER_SIZE;
            notifyAll();
        }

    }


    @Override
    synchronized public int read() throws IOException {

        int retval;

        while(mPacketSize <= 0) {
            try {
                wait(500);

                //repeat send ack packet
                BasicStreamingProtocol.writeHeader(mBuffer, 0, mSequenceNum-1, true);
                mSocket.send(mBuffer, 0, BasicStreamingProtocol.HEADER_SIZE);

            } catch (InterruptedException e) {
                LOGGER.warn("", e);
            }
        }

        retval = mBuffer[mBufferOffset++];
        if(mBufferOffset == mPacketSize) {
            //send ack packet
            BasicStreamingProtocol.writeHeader(mBuffer, 0, mSequenceNum++, true);
            mSocket.send(mBuffer, 0, BasicStreamingProtocol.HEADER_SIZE);
            mBufferOffset = BasicStreamingProtocol.HEADER_SIZE;
            mPacketSize = 0;
        }

        return retval;


    }


}
