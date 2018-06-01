package com.devsmart.rpcspeak.transmission;

public interface DatagramSocket {

    void send(byte[] buf, int offset, int size);

    /**
     * Receive a packet next packet from the underlying data stream.
     *
     * @param buf the buffer into which the recived packet will be written
     * @param offset the offset within the buffer
     * @param size the maximal size of the packet must be <= buf.length
     * @return the size of the received packet or
     */
    int receive(byte[] buf, int offset, int size);
}
