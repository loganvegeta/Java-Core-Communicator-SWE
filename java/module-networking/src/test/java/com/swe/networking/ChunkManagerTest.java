package com.swe.networking;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Vector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        final ChunkManager chunkManager = ChunkManager.getChunkManager(payloadSize);
        final PacketParser parser = PacketParser.getPacketParser();

        final String message = "Hello this is Networking Team";
        final byte[] data = message.getBytes();

        final int priority = 3;
        final int module = 0;
        final int connectionType = 1;
        final int broadcast = 1;
        final InetAddress ipAddr = InetAddress.getByName("0.0.0.0");
        final int port = 8000;
        PacketInfo info = new PacketInfo();
        info.setPriority(priority);
        info.setModule(module);
        info.setConnectionType(connectionType);
        info.setBroadcast(broadcast);
        info.setIpAddress(ipAddr);
        info.setPortNum(port);
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
        final ChunkManager chunkManager = ChunkManager.getChunkManager(payloadSize);
        final PacketParser parser = PacketParser.getPacketParser();

        final String message = "Hello this is Networking Team";
        final byte[] data = message.getBytes();
        final int priority = 3;
        final int module = 0;
        final int connectionType = 1;
        final int broadcast = 1;
        final InetAddress ipAddr = InetAddress.getByName("0.0.0.0");
        final int port = 8000;
        PacketInfo info = new PacketInfo();
        info.setPriority(priority);
        info.setModule(module);
        info.setConnectionType(connectionType);
        info.setBroadcast(broadcast);
        info.setIpAddress(ipAddr);
        info.setPortNum(port);
        info.setPayload(data);
        final Vector<byte[]> chunks = chunkManager.chunk(info, payloadSize);
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
        final ChunkManager chunkManager = ChunkManager.getChunkManager(payloadSize);
        final PacketParser parser = PacketParser.getPacketParser();

        final String message = "Hello this is Networking Team";
        final byte[] data = message.getBytes();

        final int priority = 3;
        final int module = 0;
        final int connectionType = 1;
        final int broadcast = 1;
        final InetAddress ipAddr = InetAddress.getByName("0.0.0.0");
        final int port = 8000;
        PacketInfo info = new PacketInfo();
        info.setPriority(priority);
        info.setModule(module);
        info.setConnectionType(connectionType);
        info.setBroadcast(broadcast);
        info.setIpAddress(ipAddr);
        info.setPortNum(port);
        info.setPayload(data);
        final Vector<byte[]> chunks = chunkManager.chunk(info, payloadSize);
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
            Assertions.assertEquals(chunkManager.getLastMessageId(), chunkMessageId);
        }
    }

    /**
     * Merge Chunk test.
     *
     * @throws UnknownHostException
     */
    @Test
    void mergeChunksTest() throws UnknownHostException {
        final int payloadSize = 3;
        final ChunkManager chunkManager = ChunkManager.getChunkManager(payloadSize);
        final PacketParser parser = PacketParser.getPacketParser();

        final String message = "Hello this is Networking Team";
        final byte[] data = message.getBytes();
        final int priority = 3;
        final int module = 0;
        final int connectionType = 1;
        final int broadcast = 1;
        final InetAddress ipAddr = InetAddress.getByName("0.0.0.0");
        final int port = 8000;
        PacketInfo info = new PacketInfo();
        info.setPriority(priority);
        info.setModule(module);
        info.setConnectionType(connectionType);
        info.setBroadcast(broadcast);
        info.setIpAddress(ipAddr);
        info.setPortNum(port);
        info.setPayload(data);
        final Vector<byte[]> chunks = chunkManager.chunk(info, payloadSize);
        Collections.shuffle(chunks);
        for (byte[] chunk : chunks) {
            chunkManager.addChunk(chunk);
        }
        final Vector<byte[]> mergedPkts = chunkManager.getMessageList();
        final byte[] mergedPkt = mergedPkts.get(chunkManager.getLastMessageId());
        info = parser.parsePacket(mergedPkt);
        Assertions.assertEquals(priority, info.getPriority());
        Assertions.assertEquals(module, info.getModule());
        Assertions.assertEquals(connectionType, info.getConnectionType());
        Assertions.assertEquals(broadcast, info.getBroadcast());
        Assertions.assertEquals(ipAddr, info.getIpAddress());
        Assertions.assertEquals(chunkManager.getLastMessageId(), info.getMessageId());
        final String mergedMessage = new String(info.getPayload(), StandardCharsets.UTF_8);
        Assertions.assertEquals(message, mergedMessage);
    }
}
