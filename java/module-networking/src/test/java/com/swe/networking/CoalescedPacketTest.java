package com.swe.networking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

public class CoalescedPacketTest {
    private CoalescedPacket coalescedPacket;

    @BeforeEach
    public void setup() {
        coalescedPacket = new CoalescedPacket();
    }

    @Test
    public void testInitialState() {
        assertEquals(0, coalescedPacket.getTotalSize());
        assertEquals(0, coalescedPacket.getStartTime());
        assertNull(coalescedPacket.getQueueHead());
    }

    @Test
    public void testAddingNullPacket() {
        coalescedPacket.addToQueue(null);
        assertEquals(0, coalescedPacket.getTotalSize());
        assertNull(coalescedPacket.getQueueHead());
    }

    @Test
    public void testAddingEmptyPacket() {
        byte[] emptyPacket = new byte[0];
        coalescedPacket.addToQueue(emptyPacket);
        assertEquals(0, coalescedPacket.getTotalSize());
        assertNull(coalescedPacket.getQueueHead());
    }

    @Test
    public void testAddingFirstPacket() {
        byte[] packet = new byte[100];

        long timeBeforeAdd  = System.currentTimeMillis();
        coalescedPacket.addToQueue(packet);
        long timeAfterAdd = System.currentTimeMillis();

        assertEquals(100, coalescedPacket.getTotalSize());
        assertTrue(timeBeforeAdd <= coalescedPacket.getStartTime());
        assertTrue(timeAfterAdd >= coalescedPacket.getStartTime());
        assertNotNull(coalescedPacket.getQueueHead());
    }

    @Test
    public void testAddingMultiplePackets() {
        byte[] packet1 = new byte[100];
        byte[] packet2 = new byte[50];

        coalescedPacket.addToQueue(packet1);
        coalescedPacket.addToQueue(packet2);

        assertEquals(150, coalescedPacket.getTotalSize());
        assertNotNull(coalescedPacket.getQueueHead());
    }

    @Test
    public void testNoChangeInStartTime() {
        byte[] packet1 = new byte[100];
        byte[] packet2 = new byte[50];

        coalescedPacket.addToQueue(packet1);
        long startTime1 = coalescedPacket.getStartTime();

        try{
            Thread.sleep(50);
        } catch(InterruptedException e){
            e.printStackTrace();
        }

        coalescedPacket.addToQueue(packet2);
        long startTime2 =  coalescedPacket.getStartTime();

        assertTrue(startTime1 > 0);
        assertEquals(startTime1, startTime2);
    }

    @Test
    public void testQueuePolling() {
        byte[] packet1 = new byte[]{0x1, 0x2}; // 2 bytes
        byte[] packet2 = new byte[]{0x3, 0x4, 0x5}; // 3 bytes

        coalescedPacket.addToQueue(packet1);
        coalescedPacket.addToQueue(packet2);

        assertEquals(5, coalescedPacket.getTotalSize());

        byte[] polledData1 = coalescedPacket.getQueueHead();
        assertNotNull(polledData1);
        assertArrayEquals(packet1, polledData1);
        assertEquals(3, coalescedPacket.getTotalSize());

        byte[] polledData2 = coalescedPacket.getQueueHead();
        assertNotNull(polledData2);
        assertArrayEquals(packet2, polledData2);
        assertEquals(0, coalescedPacket.getTotalSize());
    }

    @Test
    public void testEmptyQueuePolling() {
        assertNull(coalescedPacket.getQueueHead());
        assertEquals(0, coalescedPacket.getTotalSize());

        coalescedPacket.addToQueue(new byte[100]);
        coalescedPacket.getQueueHead();

        assertNull(coalescedPacket.getQueueHead());
        assertEquals(0, coalescedPacket.getTotalSize());
    }

    @Test
    public void testAddingEmptyPacketWhenQueueNotEmpty() {
        byte[] packet1 = new byte[10];

        coalescedPacket.addToQueue(packet1);
        long startTime = coalescedPacket.getStartTime();

        coalescedPacket.addToQueue(new byte[0]);

        assertEquals(10, coalescedPacket.getTotalSize());
        assertEquals(startTime, coalescedPacket.getStartTime());
    }
}
