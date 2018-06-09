package com.devsmart.rpcspeak.transmission;

/**
 *
 * Writes header on Basic Sliding Window protocol packets
 *
 * byte 0-1: 15-bit unsigned sequenceNum
 * +------------------------------------------------------------------------+
 * |  0         | 1         |  2..3 Reserved |   MSB    11 .... 0    LSB    |
 * +------------+-----------------------------------------------------------+
 * |   ACK FLAG | SYN FLAG  |                |  12-bit uint sequenceNum     |
 * |            |           |                |                              |
 * +------------------------------------------------------------------------+
 *
 */
public class BasicStreamingProtocol {


    public static final int HEADER_SIZE = 2;

    private static final int MAX_SEQUENCE_NUM = 0x1000;

    public static final int normializeSequenceNum(int input) {
        return input % MAX_SEQUENCE_NUM;
    }

    public static void writeHeader(byte[] buff, int offset, int sequenceNum, boolean isAck) {
        buff[offset + 0] = (byte) ( ((isAck ? 1 : 0) << 7) | ( 0x0F & (sequenceNum >>> 8)) );
        buff[offset + 1] = (byte) ( 0xFF & sequenceNum );
    }

    public static int readSequenceNum(byte[] buff, int offset) {
        int retval = (0x0F & buff[offset + 0]) << 8;
        retval |= (0xFF & buff[offset + 1]);

        return retval;

    }

    public static boolean readIsAck(byte[] buff, int offset) {
        return (0x80 & buff[offset + 0]) > 0;
    }

}
