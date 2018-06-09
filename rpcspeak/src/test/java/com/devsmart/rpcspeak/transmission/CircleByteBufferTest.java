package com.devsmart.rpcspeak.transmission;

import org.junit.Test;
import static org.junit.Assert.*;

public class CircleByteBufferTest {

    @Test
    public void testCapacity() {
        CircleByteBuffer buff = new CircleByteBuffer(10);
        assertEquals(10, buff.getCapacity());

        buff.put((byte) 0x3);

        assertEquals(10, buff.getCapacity());
    }

    @Test
    public void testAvailable() {
        CircleByteBuffer buff = new CircleByteBuffer(10);

        assertEquals(0, buff.getAvailable());

        buff.put((byte) 0x3);
        assertEquals(1, buff.getAvailable());

        buff.get();
        assertEquals(0, buff.getAvailable());
    }

    @Test
    public void testSinglePutGet() {
        CircleByteBuffer buff = new CircleByteBuffer(10);
        buff.put((byte) 0x3);
        assertEquals((byte) 0x3, buff.get());
    }

    @Test
    public void testMultiplePutGet() {
        CircleByteBuffer buff = new CircleByteBuffer(3);
        buff.put((byte) 0x0);
        buff.put((byte) 0x1);
        buff.put((byte) 0x2);

        assertEquals((byte) 0x0, buff.get());
        assertEquals((byte) 0x1, buff.get());
        assertEquals((byte) 0x2, buff.get());
    }

    @Test
    public void testSingleReadAcrossBoundery() {
        CircleByteBuffer buff = new CircleByteBuffer(3);
        assertTrue(buff.put((byte) 0x0));
        assertTrue(buff.put((byte) 0x1));
        assertTrue(buff.put((byte) 0x2));
        assertFalse(buff.put((byte) 0x3));
        assertEquals((byte) 0x0, buff.get());
        assertTrue(buff.put((byte) 0x3));
        assertEquals((byte) 0x1, buff.get());
        assertEquals((byte) 0x2, buff.get());
        assertEquals((byte) 0x3, buff.get());
    }

    @Test
    public void testSinglePutMultipleRead() {
        CircleByteBuffer buff = new CircleByteBuffer(3);
        buff.put((byte) 0x0);
        buff.put((byte) 0x1);
        buff.put((byte) 0x2);

        byte[] outputBuff = new byte[10];

        int bytesRead = buff.get(outputBuff, 2, outputBuff.length);
        assertEquals(3, bytesRead);
        assertEquals(0, buff.getAvailable());
        assertEquals((byte) 0x0, outputBuff[2]);
        assertEquals((byte) 0x1, outputBuff[3]);
        assertEquals((byte) 0x2, outputBuff[4]);

    }

    @Test
    public void testSinglePutMultipleReadAcrossBoundary() {
        byte[] outputBuff = new byte[10];
        CircleByteBuffer buff = new CircleByteBuffer(3);

        buff.put((byte) 0x0);
        buff.put((byte) 0x1);
        buff.put((byte) 0x2);
        buff.get();
        buff.put((byte) 0x3);
        assertEquals(3, buff.getAvailable());
        int bytesRead = buff.get(outputBuff, 0, outputBuff.length);
        assertEquals(2, bytesRead);
        assertEquals(1, buff.getAvailable());
        assertEquals((byte) 0x1, outputBuff[0]);
        assertEquals((byte) 0x2, outputBuff[1]);

    }
}
