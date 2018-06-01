package com.devsmart.rpcspeak.transmission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Random;

public class PacketLossConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(PacketLossConnection.class);

    public Random mRandom;
    public float mProbilibityOfPacketLoss = 0.01f;

    private LinkedList<byte[]> mAtoBQueue = new LinkedList<>();
    private LinkedList<byte[]> mBtoAQueue = new LinkedList<>();

    private boolean dropPacket() {
        float randomNum = mRandom.nextFloat();
        return randomNum < mProbilibityOfPacketLoss;
    }

    private class Endpoint implements DatagramSocket {

        private LinkedList<byte[]> mSendQueue;
        private LinkedList<byte[]> mReceiveQueue;

        @Override
        public void send(byte[] buf, int offset, int size) {
            if(!dropPacket()) {
                synchronized (mSendQueue) {
                    byte[] packet = new byte[size];
                    System.arraycopy(buf, offset, packet, 0, size);
                    mSendQueue.offer(packet);
                    mSendQueue.notifyAll();
                }
            }
        }

        @Override
        public int receive(byte[] buf, int offset, int size) {
            byte[] packet;

            synchronized (mReceiveQueue) {
                packet = mReceiveQueue.poll();
            }

            if(packet == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    LOGGER.warn("", e);
                }
                return 0;
            } else {
                int len = Math.min(size, packet.length);
                System.arraycopy(packet, 0, buf, offset, len);
                return len;
            }

        }
    }

    private Endpoint mEndpointA;
    private Endpoint mEndpointB;

    public PacketLossConnection(Random random) {
        mRandom = random;

        mEndpointA = new Endpoint();
        mEndpointA.mSendQueue = mAtoBQueue;
        mEndpointA.mReceiveQueue = mBtoAQueue;

        mEndpointB = new Endpoint();
        mEndpointB.mSendQueue = mBtoAQueue;
        mEndpointB.mReceiveQueue = mAtoBQueue;
    }

    public DatagramSocket getEndpointA() {
        return mEndpointA;

    }

    public DatagramSocket getEndpointB() {
        return mEndpointB;
    }


}
