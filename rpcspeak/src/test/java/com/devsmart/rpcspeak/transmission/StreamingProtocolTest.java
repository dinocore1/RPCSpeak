package com.devsmart.rpcspeak.transmission;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class StreamingProtocolTest {

    private PacketLossConnection mPacketLossConnection;

    @Before
    public void setup() {
        mPacketLossConnection = new PacketLossConnection(new Random(1));
    }


    @Test
    public void transmitDataTest() {

        DatagramSocket endpointA = mPacketLossConnection.getEndpointA();
        DatagramSocket endpointB = mPacketLossConnection.getEndpointB();

        BasicPacketStreamingSocket socketA = new BasicPacketStreamingSocket(endpointA);
        BasicPacketStreamingSocket socketB = new BasicPacketStreamingSocket(endpointB);





    }

}
