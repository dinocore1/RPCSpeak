package com.devsmart.rpcspeak.transmission;

import com.devsmart.rpcspeak.transmission.DatagramSocket;
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

    //Current sequence number (n_t) next packet to be transmitted
    private int mSequenceNum = 0;

    //Highest acknolaged sequence num (n_a)
    private int mAckedSequenceNum = -1;

    //Window size (w_t)
    private static final int WINDOW_SIZE = 10;

    //the offset within the unsent packet the next byte will be written
    private int mByteoffset = 0;

    public SlidingWindowOutputStream(int mtu, DatagramSocket socket) {
        this.mtu = mtu;
        this.mSocket = socket;
        this.mBuffer = new byte[(BasicStreamingProtocol.HEADER_SIZE + mtu) * WINDOW_SIZE];
    }

    public SlidingWindowOutputStream(DatagramSocket socket) {
        this(1024, socket);
    }

    private int bufferOffset(int sequenceNum) {
        return (BasicStreamingProtocol.HEADER_SIZE + mtu) * (sequenceNum % WINDOW_SIZE);
    }

    void ackReceived(int seqNum) {
        mSequenceNum = Math.max(mSequenceNum, seqNum);
    }

    @Override
    public synchronized void write(int i) throws IOException {
        final int offset = bufferOffset(mSequenceNum) + BasicStreamingProtocol.HEADER_SIZE + mByteoffset++;
        mBuffer[offset] = (byte) (0xFF & i);
        if(mByteoffset == mtu) {
            sendPacket();
        }
    }

    private synchronized void sendPacket() {


        while( BasicStreamingProtocol.normializeSequenceNum(mSequenceNum) > BasicStreamingProtocol.normializeSequenceNum(mAckedSequenceNum+WINDOW_SIZE)) {
            try {
                wait(500);
            } catch (InterruptedException e) {
                LOGGER.warn("", e);
            }
        }

        final int bufferOffset = bufferOffset(mSequenceNum);

        BasicStreamingProtocol.writeHeader(mBuffer, bufferOffset, mSequenceNum, false);
        mSocket.send(mBuffer, bufferOffset, BasicStreamingProtocol.HEADER_SIZE + mByteoffset - 1);
        mByteoffset = 0;
        mSequenceNum++;
    }

    @Override
    public void flush() throws IOException {

    }
}
