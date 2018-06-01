package com.devsmart.rpcspeak.transmission;

import com.devsmart.ThreadUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.*;

public class StreamingProtocolTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingProtocolTest.class);

    private PacketLossConnection mPacketLossConnection;

    @Before
    public void setup() {
        mPacketLossConnection = new PacketLossConnection(new Random(1));
    }


    @Test
    public void transmitDataTest() throws IOException {

        final int numBytes = 10000;

        DatagramSocket endpointA = mPacketLossConnection.getEndpointA();
        DatagramSocket endpointB = mPacketLossConnection.getEndpointB();

        final BasicPacketStreamingSocket socketA = new BasicPacketStreamingSocket(endpointA);
        final BasicPacketStreamingSocket socketB = new BasicPacketStreamingSocket(endpointB);


        socketA.start();
        socketB.start();

        Random r = new Random(1);
        final byte[] data = new byte[numBytes];
        r.nextBytes(data);


        ThreadUtils.IOThreads.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    socketA.getOutputStream().write(data);
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
        });


        final byte[] readData = new byte[numBytes];
        DataInputStream din = new DataInputStream(socketB.getInputStream());
        din.readFully(readData);

        assertEquals(data, din);


    }

}
