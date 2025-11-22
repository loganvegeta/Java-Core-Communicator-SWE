package com.swe.networking;

import com.swe.networking.ModuleType;
import com.swe.networking.Networking;
import com.swe.networking.MessageListener;
import com.swe.networking.ClientNode;
import com.swe.networking.PriorityQueue;
import com.swe.networking.PacketParser;
import com.swe.networking.PacketInfo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NetworkingTest {

    private Networking networking;

    private PriorityQueue priorityQueue;
    private PacketParser packetParser;
    private Topology topology;

    private final ClientNode serverNode = new ClientNode("127.0.0.1", 8000);
    private final ClientNode clientNode = new ClientNode("127.0.0.1", 8001);

    /**
     * This setup runs before each @Test.
     */
    @BeforeEach
    public void setUp() {
        networking = Networking.getNetwork();
        priorityQueue = PriorityQueue.getPriorityQueue();
        packetParser = PacketParser.getPacketParser();
        topology = Topology.getTopology(); // Get topology for setup

        priorityQueue.clear();

        networking.addUser(serverNode, serverNode);
    }

    @AfterEach
    public void tearDown() {
        topology = null;
        networking = null;
        priorityQueue = null;
        packetParser = null;
    }

    @Test
    public void testSubscribeAndCallSubscriber() {

        AtomicReference<byte[]> receivedDataRef = new AtomicReference<>();
        MessageListener chatListener = (data) -> {
            System.out.println("Received data: " + new String(data));
            receivedDataRef.set(data);
        };

        networking.subscribe(ModuleType.CHAT.ordinal(), chatListener);
        byte[] testData = "hello chat module!".getBytes();

        networking.callSubscriber(ModuleType.CHAT.ordinal(), testData);

        assertNotNull(receivedDataRef.get(), "Listener was not called");
        assertArrayEquals(testData, receivedDataRef.get(), "Listener received incorrect data");
    }

    @Test
    public void testRemoveSubscription() {
        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        MessageListener chatListener = (data) -> {
            System.out.println("Received data: " + new String(data));
            listenerCalled.set(true);
        };

        networking.subscribe(ModuleType.CHAT.ordinal(), chatListener);
        networking.removeSubscription(ModuleType.CHAT.ordinal());

        byte[] testData = "remove this !".getBytes();
        networking.callSubscriber(ModuleType.CHAT.ordinal(), testData);
        networking.removeSubscription(ModuleType.CANVAS.ordinal());

        assertFalse(listenerCalled.get(), "Listener was called after being removed");
    }

    @Test
    public void testSendDataPacketToPriorityQueue() {
        topology.addClient(clientNode);
        byte[] data = "test data".getBytes();
        System.out.println("Sending data to networking : " + new String(data));
        networking.sendData(data, new ClientNode[]{clientNode}, ModuleType.CANVAS.ordinal(), 1);
    }

    @Test
    public void testBroadcast() throws Exception {

        Field threadField = Networking.class.getDeclaredField("startThread");
        threadField.setAccessible(true);

        Thread privateSendThread = (Thread) threadField.get(networking);

        assertNotNull(privateSendThread, "Networking thread was not started");
        privateSendThread.interrupt();
        privateSendThread.join(1000);

        assertTrue(priorityQueue.isEmpty(), "Queue should be empty before test");
        byte[] data = "test broadcast data".getBytes();

        networking.broadcast(data, ModuleType.UIUX.ordinal(), 2);

        System.out.println("size of priority queue: "+priorityQueue.isEmpty());
        assertFalse(priorityQueue.isEmpty(), "Queue should be empty after broadcast");
        while(privateSendThread.isAlive()) {
            System.out.println("Thread is still running...");
            Thread.sleep(1000);
        }
        byte[] packetBytes = priorityQueue.nextPacket();
        System.out.println("Packet length: "+ Arrays.toString(packetBytes));

        assertNotNull(packetBytes);

        PacketInfo info = packetParser.parsePacket(packetBytes);
        assertEquals(1, info.getBroadcast(), "Broadcast flag should be set to 1");
        assertArrayEquals(data, info.getPayload(), "Payload data does not match");
        privateSendThread = null;
    }
}