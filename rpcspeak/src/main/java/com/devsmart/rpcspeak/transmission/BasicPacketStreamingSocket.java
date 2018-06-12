package com.devsmart.rpcspeak.transmission;

import java.io.InputStream;
import java.io.OutputStream;

public class BasicPacketStreamingSocket {

    private final DatagramSocket mSocket;
    private SlidingWindowInputStream mInputStream;
    private SlidingWindowOutputStream mOutputStream;
    private Thread mReceiveThread;
    private boolean mRunning = false;

    private final Runnable mReceiveFunction = new Runnable() {
        @Override
        public void run() {

            byte[] buffer = new byte[mInputStream.bufferSize];

            while(mRunning) {

                mOutputStream.push();
                mInputStream.push();

                int packetLen = mSocket.receive(buffer, 0, buffer.length);
                if(packetLen > 0) {
                    int seqNum = BasicStreamingProtocol.readSequenceNum(buffer, 0);

                    if (BasicStreamingProtocol.readIsAck(buffer, 0)) {
                        mOutputStream.ackReceived(seqNum);
                    } else {
                        mInputStream.packetReceived(seqNum, buffer, packetLen);
                    }
                }
            }

        }
    };

    public BasicPacketStreamingSocket(DatagramSocket socket) {
        mSocket = socket;

        mInputStream = new SlidingWindowInputStream(mSocket);
        mOutputStream = new SlidingWindowOutputStream(mSocket);
    }

    public void start() {
        mRunning = true;
        mReceiveThread = new Thread(mReceiveFunction, "ReceiveThread + " + this);
        mReceiveThread.start();

    }

    public void stop() {
        mRunning = false;

    }

    public InputStream getInputStream() {
        return mInputStream;
    }

    public OutputStream getOutputStream() {
        return mOutputStream;
    }



}
