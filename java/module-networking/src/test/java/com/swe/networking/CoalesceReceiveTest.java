package com.swe.networking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class CoalesceReceiveTest {

    private CoalesceReceive coalesceReceive;
    private Networking networking;

    // A mock message listener for testing purposes
    private static class TestMessageListener implements MessageListener {
        private final List<byte[]> receivedData = new ArrayList<>();

        @Override
        public void receiveData(byte[] data) {
            receivedData.add(data);
        }

        public List<byte[]> getReceivedData() {
            return receivedData;
        }

        public void clear() {
            receivedData.clear();
        }
    }

    @BeforeEach
    public void setUp() {
        coalesceReceive = new CoalesceReceive();
        networking = Networking.getNetwork();
    }

    @Test
    public void testSinglePacketSingleListener() {
        final byte moduleId = 1;
        final byte[] data = "test_data".getBytes();

        TestMessageListener listener = new TestMessageListener();
        networking.subscribe(moduleId, listener);

        // Create a coalesced packet
        int packetSize = 4 + 1 + data.length;
        ByteBuffer coalescedData = ByteBuffer.allocate(packetSize);
        coalescedData.putInt(packetSize);
        coalescedData.put(moduleId);
        coalescedData.put(data);
        coalescedData.flip();

        coalesceReceive.receiveCoalescedPacket(coalescedData);

        assertEquals(1, listener.getReceivedData().size());
        assertArrayEquals(data, listener.getReceivedData().get(0));

        networking.removeSubscription(moduleId);
    }

    @Test
    public void testMultiplePacketsSingleListener() {
        final byte moduleId = 2;
        final byte[] data1 = "test_data_1".getBytes();
        final byte[] data2 = "test_data_2".getBytes();

        TestMessageListener listener = new TestMessageListener();
        networking.subscribe(moduleId, listener);

        // Create a coalesced packet with two packets for the same module
        int packetSize1 = 4 + 1 + data1.length;
        int packetSize2 = 4 + 1 + data2.length;
        ByteBuffer coalescedData = ByteBuffer.allocate(packetSize1 + packetSize2);

        coalescedData.putInt(packetSize1);
        coalescedData.put(moduleId);
        coalescedData.put(data1);

        coalescedData.putInt(packetSize2);
        coalescedData.put(moduleId);
        coalescedData.put(data2);

        coalescedData.flip();

        coalesceReceive.receiveCoalescedPacket(coalescedData);

        assertEquals(2, listener.getReceivedData().size());
        assertArrayEquals(data1, listener.getReceivedData().get(0));
        assertArrayEquals(data2, listener.getReceivedData().get(1));

        networking.removeSubscription(moduleId);
    }

    @Test
    public void testSinglePacketMultipleListeners() {
        final byte moduleId1 = 3;
        final byte moduleId2 = 4;
        final byte[] data = "test_data".getBytes();

        TestMessageListener listener1 = new TestMessageListener();
        TestMessageListener listener2 = new TestMessageListener();
        networking.subscribe(moduleId1, listener1);
        networking.subscribe(moduleId2, listener2);

        // Create a coalesced packet for the first module
        int packetSize = 4 + 1 + data.length;
        ByteBuffer coalescedData = ByteBuffer.allocate(packetSize);
        coalescedData.putInt(packetSize);
        coalescedData.put(moduleId1);
        coalescedData.put(data);
        coalescedData.flip();

        coalesceReceive.receiveCoalescedPacket(coalescedData);

        assertEquals(1, listener1.getReceivedData().size());
        assertArrayEquals(data, listener1.getReceivedData().get(0));
        assertTrue(listener2.getReceivedData().isEmpty());

        networking.removeSubscription(moduleId1);
        networking.removeSubscription(moduleId2);
    }

    @Test
    public void testMultiplePacketsMultipleListeners() {
        final byte moduleId1 = 5;
        final byte moduleId2 = 6;
        final byte[] data1 = "test_data_1".getBytes();
        final byte[] data2 = "test_data_2".getBytes();

        TestMessageListener listener1 = new TestMessageListener();
        TestMessageListener listener2 = new TestMessageListener();
        networking.subscribe(moduleId1, listener1);
        networking.subscribe(moduleId2, listener2);

        // Create a coalesced packet with one packet for each module
        int packetSize1 = 4 + 1 + data1.length;
        int packetSize2 = 4 + 1 + data2.length;
        ByteBuffer coalescedData = ByteBuffer.allocate(packetSize1 + packetSize2);

        coalescedData.putInt(packetSize1);
        coalescedData.put(moduleId1);
        coalescedData.put(data1);

        coalescedData.putInt(packetSize2);
        coalescedData.put(moduleId2);
        coalescedData.put(data2);

        coalescedData.flip();

        coalesceReceive.receiveCoalescedPacket(coalescedData);

        assertEquals(1, listener1.getReceivedData().size());
        assertArrayEquals(data1, listener1.getReceivedData().get(0));

        assertEquals(1, listener2.getReceivedData().size());
        assertArrayEquals(data2, listener2.getReceivedData().get(0));

        networking.removeSubscription(moduleId1);
        networking.removeSubscription(moduleId2);
    }

    @Test
    public void testEmptyBytesNoReceive() {
        final byte moduleId1 = 7;
        final byte moduleId2 = 8;

        TestMessageListener listener1 = new TestMessageListener();
        TestMessageListener listener2 = new TestMessageListener();
        networking.subscribe(moduleId1, listener1);
        networking.subscribe(moduleId2, listener2);

        ByteBuffer emptyBuffer = ByteBuffer.allocate(0);
        coalesceReceive.receiveCoalescedPacket(emptyBuffer);

        assertTrue(listener1.getReceivedData().isEmpty());
        assertTrue(listener2.getReceivedData().isEmpty());

        networking.removeSubscription(moduleId1);
        networking.removeSubscription(moduleId2);
    }
}