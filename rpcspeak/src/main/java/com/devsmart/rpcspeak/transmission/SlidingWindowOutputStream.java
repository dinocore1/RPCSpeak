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

    public final int mtu;
    private final DatagramSocket mSocket;
    private final byte[] mBuffer;
    private final int[] mPacketSizes;

    //Current sequence number (n_t) next packet to be transmitted
    private int mN_t = 1;

    //Highest acknolaged sequence num (n_a)
    private int mN_a = -1;

    //Window size (w_t)
    private static final int WINDOW_SIZE = 1024;

    //the offset within the unsent packet the next byte will be written
    private int mByteoffset = 0;

    public SlidingWindowOutputStream(int mtu, DatagramSocket socket) {
        this.mtu = mtu;
        this.mSocket = socket;
        this.mBuffer = new byte[(BasicStreamingProtocol.HEADER_SIZE + mtu) * WINDOW_SIZE];
        this.mPacketSizes = new int[WINDOW_SIZE];
    }

    public SlidingWindowOutputStream(DatagramSocket socket) {
        this(1024, socket);
    }

    private int bufferOffset(int sequenceNum) {
        return (BasicStreamingProtocol.HEADER_SIZE + mtu) * (sequenceNum % WINDOW_SIZE);
    }

    synchronized void ackReceived(int seqNum) {
        mN_a = Math.max(mN_a, seqNum);
        notifyAll();
    }

    @Override
    public synchronized void write(int i) throws IOException {

        while(BasicStreamingProtocol.normializeSequenceNum(mN_t) > BasicStreamingProtocol.normializeSequenceNum(mN_a + WINDOW_SIZE-1)) {
            sendPacket(mN_a + 1);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LOGGER.warn("", e);
            }
        }

        final int offset = bufferOffset(mN_t) + BasicStreamingProtocol.HEADER_SIZE + mByteoffset++;
        mBuffer[offset] = (byte) (0xFF & i);
        if(mByteoffset == mtu - BasicStreamingProtocol.HEADER_SIZE) {
            //finalize packet by writing the header
            BasicStreamingProtocol.writeHeader(mBuffer, bufferOffset(mN_t), mN_t, false);
            mPacketSizes[mN_t % WINDOW_SIZE] = mByteoffset;
            sendPacket(mN_t);
            mByteoffset = 0;
            mN_t++;
        }
    }


    private void sendPacket(int seqNum) {
        final int bufferOffset = bufferOffset(seqNum);
        mSocket.send(mBuffer, bufferOffset, mPacketSizes[seqNum % WINDOW_SIZE] + BasicStreamingProtocol.HEADER_SIZE);
    }

    @Override
    public void flush() throws IOException {

    }
}
