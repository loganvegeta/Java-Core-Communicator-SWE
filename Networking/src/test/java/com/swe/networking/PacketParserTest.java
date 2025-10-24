package com.swe.networking;

import org.junit.Test;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.*;

public class PacketParserTest {

    @Test
    public void testTypeExtraction() throws UnknownHostException {
        PacketParser parser = PacketParser.getPacketParser();
        InetAddress ip = InetAddress.getByName("0.0.0.0");
        PacketInfo ds = new PacketInfo();
        ds.setType(2);
        ds.setPriority(0);
        ds.setModule(0);
        ds.setConnectionType(0);
        ds.setBroadcast(0);
        ds.setIpAddress(ip);
        ds.setPortNum(0);
        ds.setMessageId(0);
        ds.setChunkNum(0);
        ds.setChunkLength(0);
        ds.setPayload(new byte[0]);
        byte[] pkt = parser.createPkt(ds);
        PacketInfo info = parser.parsePacket(pkt);
        assertEquals(2, info.getType());
    }

    @Test
    public void testPriorityExtraction() throws UnknownHostException {
        PacketParser parser = PacketParser.getPacketParser();
        InetAddress ip = InetAddress.getByName("0.0.0.0");
        PacketInfo ds = new PacketInfo();
        ds.setType(0);
        ds.setPriority(5);
        ds.setModule(0);
        ds.setConnectionType(0);
        ds.setBroadcast(0);
        ds.setIpAddress(ip);
        ds.setPortNum(0);
        ds.setMessageId(0);
        ds.setChunkNum(0);
        ds.setChunkLength(0);
        ds.setPayload(new byte[0]);
        byte[] pkt = parser.createPkt(ds);
        PacketInfo info = parser.parsePacket(pkt);
        assertEquals(5, info.getPriority());
    }

    @Test
    public void testModuleExtraction() throws UnknownHostException {
        PacketParser parser = PacketParser.getPacketParser();
        InetAddress ip = InetAddress.getByName("0.0.0.0");
        PacketInfo ds = new PacketInfo();
        ds.setType(0);
        ds.setPriority(0);
        ds.setModule(9); // 9 = 1001 (binary)
        ds.setConnectionType(0);
        ds.setBroadcast(0);
        ds.setIpAddress(ip);
        ds.setPortNum(0);
        ds.setMessageId(0);
        ds.setChunkNum(0);
        ds.setChunkLength(0);
        ds.setPayload(new byte[0]);
        byte[] pkt = parser.createPkt(ds);
        PacketInfo info = parser.parsePacket(pkt);
        assertEquals(9, info.getModule());
    }

    @Test
    public void testConnectionTypeExtraction() throws UnknownHostException {
        PacketParser parser = PacketParser.getPacketParser();
        InetAddress ip = InetAddress.getByName("0.0.0.0");
        PacketInfo ds = new PacketInfo();
        ds.setType(0);
        ds.setPriority(0);
        ds.setModule(0);
        ds.setConnectionType(6);
        ds.setBroadcast(0);
        ds.setIpAddress(ip);
        ds.setPortNum(0);
        ds.setMessageId(0);
        ds.setChunkNum(0);
        ds.setChunkLength(0);
        ds.setPayload(new byte[0]);
        byte[] pkt = parser.createPkt(ds);
        PacketInfo info = parser.parsePacket(pkt);
        assertEquals(6, info.getConnectionType());
    }

    @Test
    public void testBroadcastExtraction() throws UnknownHostException {
        PacketParser parser = PacketParser.getPacketParser();
        InetAddress ip = InetAddress.getByName("0.0.0.0");
        PacketInfo ds = new PacketInfo();
        ds.setType(0);
        ds.setPriority(0);
        ds.setModule(0);
        ds.setConnectionType(0);
        ds.setBroadcast(1);
        ds.setIpAddress(ip);
        ds.setPortNum(0);
        ds.setMessageId(0);
        ds.setChunkNum(0);
        ds.setChunkLength(0);
        ds.setPayload(new byte[0]);
        byte[] pkt = parser.createPkt(ds);
        PacketInfo info = parser.parsePacket(pkt);
        assertEquals(1, info.getBroadcast());
    }

    @Test
    public void testIpAddressExtraction() throws UnknownHostException {
        PacketParser parser = PacketParser.getPacketParser();
        InetAddress ip = InetAddress.getByName("192.168.1.50");
        PacketInfo ds = new PacketInfo();
        ds.setType(0);
        ds.setPriority(0);
        ds.setModule(0);
        ds.setConnectionType(0);
        ds.setBroadcast(0);
        ds.setIpAddress(ip);
        ds.setPortNum(0);
        ds.setMessageId(0);
        ds.setChunkNum(0);
        ds.setChunkLength(0);
        ds.setPayload(new byte[0]);
        byte[] pkt = parser.createPkt(ds);
        PacketInfo info = parser.parsePacket(pkt);
        assertEquals(ip, info.getIpAddress());
    }

    @Test
    public void testPortNumExtraction() throws UnknownHostException {
        PacketParser parser = PacketParser.getPacketParser();
        InetAddress ip = InetAddress.getByName("0.0.0.0");
        int port = 8080;
        PacketInfo ds = new PacketInfo();
        ds.setType(0);
        ds.setPriority(0);
        ds.setModule(0);
        ds.setConnectionType(0);
        ds.setBroadcast(0);
        ds.setIpAddress(ip);
        ds.setPortNum(port);
        ds.setMessageId(0);
        ds.setChunkNum(0);
        ds.setChunkLength(0);
        ds.setPayload(new byte[0]);
        byte[] pkt = parser.createPkt(ds);
        PacketInfo info = parser.parsePacket(pkt);
        assertEquals(port, info.getPortNum());
    }

    @Test
    public void testMessageIdExtraction() throws UnknownHostException {
        PacketParser parser = PacketParser.getPacketParser();
        InetAddress ip = InetAddress.getByName("0.0.0.0");
        int messageId = 123456;
        PacketInfo ds = new PacketInfo();
        ds.setType(0);
        ds.setPriority(0);
        ds.setModule(0);
        ds.setConnectionType(0);
        ds.setBroadcast(0);
        ds.setIpAddress(ip);
        ds.setPortNum(0);
        ds.setMessageId(messageId);
        ds.setChunkNum(0);
        ds.setChunkLength(0);
        ds.setPayload(new byte[0]);
        byte[] pkt = parser.createPkt(ds);
        PacketInfo info = parser.parsePacket(pkt);
        assertEquals(messageId, info.getMessageId());
    }

    @Test
    public void testChunkNumExtraction() throws UnknownHostException {
        PacketParser parser = PacketParser.getPacketParser();
        InetAddress ip = InetAddress.getByName("0.0.0.0");
        int chunkNum = 654321;
        PacketInfo ds = new PacketInfo();
        ds.setType(0);
        ds.setPriority(0);
        ds.setModule(0);
        ds.setConnectionType(0);
        ds.setBroadcast(0);
        ds.setIpAddress(ip);
        ds.setPortNum(0);
        ds.setMessageId(0);
        ds.setChunkNum(chunkNum);
        ds.setChunkLength(0);
        ds.setPayload(new byte[0]);
        byte[] pkt = parser.createPkt(ds);
        PacketInfo info = parser.parsePacket(pkt);
        assertEquals(chunkNum, info.getChunkNum());
    }

    @Test
    public void testChunkLengthExtraction() throws UnknownHostException {
        PacketParser parser = PacketParser.getPacketParser();
        InetAddress ip = InetAddress.getByName("0.0.0.0");
        int chunkLength = 12345678;
        PacketInfo ds = new PacketInfo();
        ds.setType(0);
        ds.setPriority(0);
        ds.setModule(0);
        ds.setConnectionType(0);
        ds.setBroadcast(0);
        ds.setIpAddress(ip);
        ds.setPortNum(0);
        ds.setMessageId(0);
        ds.setChunkNum(0);
        ds.setChunkLength(chunkLength);
        ds.setPayload(new byte[0]);
        byte[] pkt = parser.createPkt(ds);
        PacketInfo info = parser.parsePacket(pkt);
        assertEquals(chunkLength, info.getChunkLength());
    }

    @Test
    public void testPayloadExtraction() throws UnknownHostException {
        PacketParser parser = PacketParser.getPacketParser();
        InetAddress ip = InetAddress.getByName("0.0.0.0");
        byte[] payload = {10, 20, 30, 40};
        PacketInfo ds = new PacketInfo();
        ds.setType(0);
        ds.setPriority(0);
        ds.setModule(0);
        ds.setConnectionType(0);
        ds.setBroadcast(0);
        ds.setIpAddress(ip);
        ds.setPortNum(0);
        ds.setMessageId(0);
        ds.setChunkNum(0);
        ds.setChunkLength(0);
        ds.setPayload(payload);
        byte[] pkt = parser.createPkt(ds);
        PacketInfo info = parser.parsePacket(pkt);
        assertArrayEquals(payload, info.getPayload());
    }

    @Test
    public void testPkt() throws UnknownHostException {
        PacketParser parser = PacketParser.getPacketParser();

        int type = 3;
        int priority = 7;
        int module = 15;
        int connectionType = 5;
        int broadcast = 1;
        InetAddress ip = InetAddress.getByName("10.0.0.5");
        int port = 65535;
        int MessageId = 12345678;
        int ChunkNum = 87654321;
        int ChunkLength = 1024;
        byte[] data = {1, 2, 3, 4, 5};

        PacketInfo ds = new PacketInfo();
        ds.setType(type);
        ds.setPriority(priority);
        ds.setModule(module);
        ds.setConnectionType(connectionType);
        ds.setBroadcast(broadcast);
        ds.setIpAddress(ip);
        ds.setPortNum(port);
        ds.setMessageId(MessageId);
        ds.setChunkNum(ChunkNum);
        ds.setChunkLength(ChunkLength);
        ds.setPayload(data);

        byte[] pkt = parser.createPkt(ds);

        PacketInfo info = parser.parsePacket(pkt);

        assertEquals(type, info.getType());
        assertEquals(priority, info.getPriority());
        assertEquals(module, info.getModule());
        assertEquals(connectionType, info.getConnectionType());
        assertEquals(broadcast, info.getBroadcast());
        assertEquals(ip, info.getIpAddress());
        assertEquals(port, info.getPortNum());
        assertEquals(MessageId, info.getMessageId());
        assertEquals(ChunkNum, info.getChunkNum());
        assertEquals(ChunkLength, info.getChunkLength());
        assertArrayEquals(data, info.getPayload());
    }
}