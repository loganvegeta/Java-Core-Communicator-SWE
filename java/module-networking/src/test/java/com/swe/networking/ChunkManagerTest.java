package com.swe.networking;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;

/**
 * Test cases for chunk manager.
 */
public class ChunkManagerTest {
    /**
     * Chunk message test.
     */
    @Test
    void messageChunkingTest() throws UnknownHostException {
        final int payloadSize = 4;
        final ChunkManager chunkManager = new ChunkManager(payloadSize);
        final PacketParser parser = PacketParser.getPacketParser();

        final String message = "Hello this is Networking Team";
        final byte[] data = message.getBytes();

        final int priority = 3;
        final int module = 0;
        final int connectionType = 1;
        final int broadcast = 1;
        final InetAddress ipAddr = InetAddress.getByName("0.0.0.0");
        final int port = 8000;
        final int messageId = 3;
        PacketInfo info = new PacketInfo();
        info.setPriority(priority);
        info.setModule(module);
        info.setConnectionType(connectionType);
        info.setBroadcast(broadcast);
        info.setIpAddress(ipAddr);
        info.setPortNum(port);
        info.setMessageId(messageId);
        info.setPayload(data);

        final Vector<byte[]> chunks = chunkManager.chunk(info);
        String chunkMsg;
        String expectedMsg;
        int index = 0;
        // 0
        info = parser.parsePacket(chunks.get(index));
        chunkMsg = new String(info.getPayload(), StandardCharsets.UTF_8);
        expectedMsg = "Hell";
        Assertions.assertEquals(expectedMsg, chunkMsg);
        index++;
        // 1
        info = parser.parsePacket(chunks.get(index));
        chunkMsg = new String(info.getPayload(), StandardCharsets.UTF_8);
        expectedMsg = "o th";
        Assertions.assertEquals(expectedMsg, chunkMsg);
        index++;
        // 2
        info = parser.parsePacket(chunks.get(index));
        chunkMsg = new String(info.getPayload(), StandardCharsets.UTF_8);
        expectedMsg = "is i";
        Assertions.assertEquals(expectedMsg, chunkMsg);
        index++;
        // 3
        info = parser.parsePacket(chunks.get(index));
        chunkMsg = new String(info.getPayload(), StandardCharsets.UTF_8);
        expectedMsg = "s Ne";
        Assertions.assertEquals(expectedMsg, chunkMsg);
        index++;
        // 4
        info = parser.parsePacket(chunks.get(index));
        chunkMsg = new String(info.getPayload(), StandardCharsets.UTF_8);
        expectedMsg = "twor";
        Assertions.assertEquals(expectedMsg, chunkMsg);
        index++;
        // 5
        info = parser.parsePacket(chunks.get(index));
        chunkMsg = new String(info.getPayload(), StandardCharsets.UTF_8);
        expectedMsg = "king";
        Assertions.assertEquals(expectedMsg, chunkMsg);
        index++;
        // 6
        info = parser.parsePacket(chunks.get(index));
        chunkMsg = new String(info.getPayload(), StandardCharsets.UTF_8);
        expectedMsg = " Tea";
        Assertions.assertEquals(expectedMsg, chunkMsg);
        index++;
        // 7
        info = parser.parsePacket(chunks.get(index));
        chunkMsg = new String(info.getPayload(), StandardCharsets.UTF_8);
        expectedMsg = "m";
        Assertions.assertEquals(expectedMsg, chunkMsg);
    }

    /**
     * Chunking number test.
     */
    @Test
    void chunkNumChunkingTest() throws UnknownHostException {
        final int payloadSize = 3;
        final ChunkManager chunkManager = new ChunkManager(payloadSize);
        final PacketParser parser = PacketParser.getPacketParser();

        final String message = "Hello this is Networking Team";
        final byte[] data = message.getBytes();
        final int priority = 3;
        final int module = 0;
        final int connectionType = 1;
        final int broadcast = 1;
        final InetAddress ipAddr = InetAddress.getByName("0.0.0.0");
        final int port = 8000;
        final int messageId = 3;
        PacketInfo info = new PacketInfo();
        info.setPriority(priority);
        info.setModule(module);
        info.setConnectionType(connectionType);
        info.setBroadcast(broadcast);
        info.setIpAddress(ipAddr);
        info.setPortNum(port);
        info.setMessageId(messageId);
        info.setPayload(data);
        final Vector<byte[]> chunks = chunkManager.chunk(info);
        for (int expectedChunkNum = 0; expectedChunkNum < chunks.size(); expectedChunkNum++) {
            info = parser.parsePacket(chunks.get(expectedChunkNum));
            final int chunkNum = info.getChunkNum();
            Assertions.assertEquals(expectedChunkNum, chunkNum);
        }
    }

    /**
     * Checking other fields.
     */
    @Test
    void constFieldChunkingTest() throws UnknownHostException {
        final int payloadSize = 6;
        final ChunkManager chunkManager = new ChunkManager(payloadSize);
        final PacketParser parser = PacketParser.getPacketParser();

        final String message = "Hello this is Networking Team";
        final byte[] data = message.getBytes();

        final int priority = 3;
        final int module = 0;
        final int connectionType = 1;
        final int broadcast = 1;
        final InetAddress ipAddr = InetAddress.getByName("0.0.0.0");
        final int port = 8000;
        final int messageId = 3;
        PacketInfo info = new PacketInfo();
        info.setPriority(priority);
        info.setModule(module);
        info.setConnectionType(connectionType);
        info.setBroadcast(broadcast);
        info.setIpAddress(ipAddr);
        info.setPortNum(port);
        info.setMessageId(messageId);
        info.setPayload(data);
        final Vector<byte[]> chunks = chunkManager.chunk(info);
        for (byte[] chunk : chunks) {
            info = parser.parsePacket(chunk);
            final int chunkPriority = info.getPriority();
            final int chunkModule = info.getModule();
            final int chunkConnectionType = info.getConnectionType();
            final int chunkBroadcast = info.getBroadcast();
            final InetAddress chunkIp = info.getIpAddress();
            final int chunkPort = info.getPortNum();
            final int chunkMessageId = info.getMessageId();
            Assertions.assertEquals(priority, chunkPriority);
            Assertions.assertEquals(module, chunkModule);
            Assertions.assertEquals(connectionType, chunkConnectionType);
            Assertions.assertEquals(broadcast, chunkBroadcast);
            Assertions.assertEquals(ipAddr, chunkIp);
            Assertions.assertEquals(port, chunkPort);
            Assertions.assertEquals(messageId, chunkMessageId);
        }
    }

    /**
     * Merge Chunk test.
     */
    @Test
    void mergeChunksTest() throws UnknownHostException, IllegalArgumentException {
        final int payloadSize = 3;
        final ChunkManager chunkManager = new ChunkManager(payloadSize);
        final PacketParser parser = PacketParser.getPacketParser();

        final String message = "Hello this is Networking Team";
        final byte[] data = message.getBytes();
        final int priority = 3;
        final int module = 0;
        final int connectionType = 1;
        final int broadcast = 1;
        final InetAddress ipAddr = InetAddress.getByName("0.0.0.0");
        final int port = 8000;
        final int messageId = 3;
        PacketInfo info = new PacketInfo();
        info.setPriority(priority);
        info.setModule(module);
        info.setConnectionType(connectionType);
        info.setBroadcast(broadcast);
        info.setIpAddress(ipAddr);
        info.setPortNum(port);
        info.setMessageId(messageId);
        info.setPayload(data);
        final Vector<byte[]> chunks = chunkManager.chunk(info);
        Collections.shuffle(chunks);
        final byte[] mergedPkt = chunkManager.mergeChunks(chunks);
        info = parser.parsePacket(mergedPkt);
        Assertions.assertEquals(priority, info.getPriority());
        Assertions.assertEquals(module, info.getModule());
        Assertions.assertEquals(connectionType, info.getConnectionType());
        Assertions.assertEquals(broadcast, info.getBroadcast());
        Assertions.assertEquals(ipAddr, info.getIpAddress());
        Assertions.assertEquals(messageId, info.getMessageId());
        final String mergedMessage = new String(info.getPayload(), StandardCharsets.UTF_8);
        Assertions.assertEquals(message, mergedMessage);
    }

    /**
     * Group chunk test.
     */
    @Test
    void mapChunksTest() throws  UnknownHostException, IllegalArgumentException {
        final int payloadSize = 3;
        final ChunkManager chunkManager = new ChunkManager(payloadSize);
        final PacketParser parser = PacketParser.getPacketParser();

        final String message = "This is Networking Team";
        final byte[] data = message.getBytes();
        final InetAddress ipAddr = InetAddress.getByName("0.0.0.0");
        final int port = 8000;
        final int messageId = 3;
        final PacketInfo info = new PacketInfo();
        info.setIpAddress(ipAddr);
        info.setPortNum(port);
        info.setMessageId(messageId);
        info.setPayload(data);
        final Vector<byte[]> chunks = chunkManager.chunk(info);
        Collections.shuffle(chunks);


        final String newMessage = "What is this team?";
        final byte[] newData = newMessage.getBytes();
        final int newMessageId = 4;
        final PacketInfo newInfo = new PacketInfo();
        newInfo.setIpAddress(ipAddr);
        newInfo.setPortNum(port);
        newInfo.setMessageId(newMessageId);
        newInfo.setPayload(newData);
        final Vector<byte[]> newChunks = chunkManager.chunk(newInfo);
        Collections.shuffle(newChunks);
        final Vector<byte[]> allChunks = new Vector<>();
        allChunks.addAll(chunks);
        allChunks.addAll(newChunks);
        final Map<Integer, Vector<byte[]>> groupedChunks = chunkManager.groupChunks(allChunks);
        for (Vector<byte[]> chunkGroup: groupedChunks.values()) {
            final byte[] mergedPkt = chunkManager.mergeChunks(chunkGroup);
            final PacketInfo mergedChunkInfo = parser.parsePacket(mergedPkt);
            final String msg = new String(mergedChunkInfo.getPayload(), StandardCharsets.UTF_8);
            final int msgId = mergedChunkInfo.getMessageId();
            if (msgId == messageId) {
                Assertions.assertEquals(message, msg);
            } else if (msgId == newMessageId) {
                Assertions.assertEquals(newMessage, msg);
            } else {
                throw new UnknownError("message id " + msgId + " is not present");
            }
        }
    }

    /**
     * Illegal argument test.
     */
    @Test
    void illegalArgumentTest() throws IllegalArgumentException, UnknownHostException {
        final Vector<byte[]> emptyChunkList = new Vector<>();
        final int payloadSize = 3;
        final ChunkManager chunkManager = new ChunkManager(payloadSize);
        boolean errorDetected;
        try {
            chunkManager.mergeChunks(emptyChunkList);
            errorDetected = false;
        } catch (IllegalArgumentException e) {
            errorDetected = true;
        }
        Assertions.assertTrue(errorDetected);
    }

    /**
     * invalid number of chunks test.
     */
    @Test
    void invalidNumberOfChunksTest() throws  IllegalArgumentException, UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final ChunkManager chunkManager = new ChunkManager(3);
        final PacketInfo info1 = new PacketInfo();
        final PacketInfo info2 = new PacketInfo();
        final int chunkLength = 3;
        final InetAddress ipAddr = InetAddress.getByName("0.0.0.0");
        info1.setChunkLength(chunkLength);
        info1.setChunkNum(0);
        info1.setIpAddress(ipAddr);
        info2.setChunkLength(chunkLength);
        info2.setChunkNum(1);
        info2.setIpAddress(ipAddr);
        final byte[] pkt1 = parser.createPkt(info1);
        final byte[] pkt2 = parser.createPkt(info2);
        final Vector<byte[]> chunkList = new Vector<>();
        chunkList.add(pkt1);
        chunkList.add(pkt2);
        boolean errorDetected;
        try {
            chunkManager.mergeChunks(chunkList);
            errorDetected = true;
        } catch (IllegalArgumentException e) {
            errorDetected = true;
        }
        Assertions.assertTrue(errorDetected);
    }
}
