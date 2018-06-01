package com.devsmart.rpcspeak.transmission;

import org.junit.Test;

import static org.junit.Assert.*;

public class PacketTest {


    @Test
    public void testHeaderPacket() {
        byte[] buff = new byte[2];
        BasicStreamingProtocol.writeHeader(buff, 0, 0, false);

        assertFalse(BasicStreamingProtocol.readIsAck(buff, 0));

    }

    @Test
    public void testHeaderAckPacket() {
        byte[] buff = new byte[2];
        BasicStreamingProtocol.writeHeader(buff, 0, 0, true);

        assertTrue(BasicStreamingProtocol.readIsAck(buff, 0));
    }

    @Test
    public void testHeaderPacketSeq() {
        byte[] buff = new byte[2];
        BasicStreamingProtocol.writeHeader(buff, 0, 256, false);

        assertFalse(BasicStreamingProtocol.readIsAck(buff, 0));
        assertEquals(256, BasicStreamingProtocol.readSequenceNum(buff, 0));
    }

    @Test
    public void testHeaderPacketSeqAck() {
        byte[] buff = new byte[2];
        BasicStreamingProtocol.writeHeader(buff, 0, 256, true);

        assertTrue(BasicStreamingProtocol.readIsAck(buff, 0));
        assertEquals(256, BasicStreamingProtocol.readSequenceNum(buff, 0));
    }

}
