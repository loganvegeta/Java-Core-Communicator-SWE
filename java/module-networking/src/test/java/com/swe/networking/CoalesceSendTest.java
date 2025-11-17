package com.swe.networking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.text.AttributeSet;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CoalesceSendTest {
    private CoalesceSend coalesceSend;
    private InetAddress destA, destB;
    private int portA, portB;
    private String destinationA,  destinationB;

    private int maxSize, maxTime;

    @BeforeEach
    public void setUp() throws Exception {
        coalesceSend = new CoalesceSend();

        destA = InetAddress.getByName("1.1.1.1");
        destB = InetAddress.getByName("2.2.2.2");

        portA = 80;
        portB = 90;

        destinationA = destA.getHostAddress() + ":" + portA;
        destinationB = destB.getHostAddress() +  ":" + portB;

        maxSize = (int) getPrivateField("maxSize");
        maxTime = (int) getPrivateField("maxTime");
    }

    private Map<String, CoalescedPacket> getInternalMap() throws NoSuchFieldException, IllegalAccessException {
        Field mapField = CoalesceSend.class.getDeclaredField("coalescedPackets");
        mapField.setAccessible(true);
        return (Map<String, CoalescedPacket>) mapField.get(coalesceSend);
    }

    private Object getPrivateField(String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = CoalesceSend.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(coalesceSend);
    }

    @Test
    public void testInitialState() throws Exception {
        Map<String, CoalescedPacket> coalescedPacketMap = getInternalMap();
        assertTrue(coalescedPacketMap.isEmpty());
    }

    @Test
    public void testOnePacketAddition() throws Exception {
        Map<String, CoalescedPacket> coalescedPacketMap = getInternalMap();

        byte[] data = new byte[100];
        coalesceSend.handlePacket(data, destA, portA, (byte) 0x01);

        assertEquals(1, coalescedPacketMap.size());
        assertTrue(coalescedPacketMap.containsKey(destinationA));

        CoalescedPacket coalescedPacket = coalescedPacketMap.get(destinationA);

        assertNotNull(coalescedPacket);
        assertEquals(105, coalescedPacket.getTotalSize());
    }

    @Test
    public void testPacketGroupingByDestination() throws Exception {
        Map<String, CoalescedPacket> coalescedPacketMap = getInternalMap();

        coalesceSend.handlePacket(new byte[10], destA, portA, (byte) 0x01);
        coalesceSend.handlePacket(new byte[20], destA, portA, (byte) 0x02);
        coalesceSend.handlePacket(new byte[30], destA, portA, (byte) 0x03);

        assertEquals(1, coalescedPacketMap.size());
        assertTrue(coalescedPacketMap.containsKey(destinationA));

        CoalescedPacket coalescedPacket = coalescedPacketMap.get(destinationA);

        assertNotNull(coalescedPacket);
        assertEquals(75, coalescedPacket.getTotalSize());
    }

    @Test
    public void testPacketAdditionAtDifferentDestinations() throws Exception {
        Map<String, CoalescedPacket> coalescedPacketMap = getInternalMap();

        coalesceSend.handlePacket(new byte[10], destA, portA, (byte) 0x01);
        coalesceSend.handlePacket(new byte[20], destB, portB, (byte) 0x02);

        assertEquals(2, coalescedPacketMap.size());
        assertTrue(coalescedPacketMap.containsKey(destinationA));
        assertTrue(coalescedPacketMap.containsKey(destinationB));

        CoalescedPacket coalescedPacket1 = coalescedPacketMap.get(destinationA);

        assertNotNull(coalescedPacket1);
        assertEquals(15, coalescedPacket1.getTotalSize());

        CoalescedPacket coalescedPacket2 = coalescedPacketMap.get(destinationB);

        assertNotNull(coalescedPacket2);
        assertEquals(25, coalescedPacket2.getTotalSize());
    }

    @Test
    public void testNoCoalescingOfLargePackets() throws Exception {
        Map<String, CoalescedPacket> coalescedPacketMap = getInternalMap();

        coalesceSend.handlePacket(new byte[2048], destA, portA, (byte) 0x01);

        assertEquals(0, coalescedPacketMap.size());
    }

    @Test
    public void testCumulativeSizeFlush() throws Exception {
        Map<String, CoalescedPacket> coalescedPacketMap = getInternalMap();

        byte[] data = new byte[(maxSize / 2) - 10];

        coalesceSend.handlePacket(data, destA, portA, (byte) 0x01);
        coalesceSend.handlePacket(data, destA, portA, (byte) 0x01);

        assertEquals(1, coalescedPacketMap.size());

        coalesceSend.handlePacket(new byte[10], destA, portA, (byte) 0x01);

        assertEquals(0, coalescedPacketMap.size());
    }

    @Test
    public void testTimeFlush() throws Exception {
        Map<String, CoalescedPacket> coalescedPacketMap = getInternalMap();

        coalesceSend.handlePacket(new byte[10], destA, portA, (byte) 0x01);

        assertEquals(1, coalescedPacketMap.size());

        Thread.sleep(maxTime + 10);

        coalesceSend.checkTimeout();

        assertEquals(0, coalescedPacketMap.size());
    }

    @Test
    public void testNoTimeFlushWithoutTimeout() throws Exception {
        Map<String, CoalescedPacket> coalescedPacketMap = getInternalMap();

        coalesceSend.handlePacket(new byte[10], destA, portA, (byte) 0x01);

       assertEquals(1, coalescedPacketMap.size());

       Thread.sleep(maxTime / 2);

       coalesceSend.checkTimeout();

       assertEquals(1, coalescedPacketMap.size());
    }

}
