package com.swe.networking;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PacketParserTest {

    @Test
    public void testLengthExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final PacketInfo ds = new PacketInfo();
        ds.setLength(22);
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
        final byte[] pkt = parser.createPkt(ds);
        final PacketInfo info = parser.parsePacket(pkt);
        assertEquals(22, info.getLength());
    }

    @Test
    public void testTypeExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final PacketInfo ds = new PacketInfo();
        ds.setLength(22);
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
        final byte[] pkt = parser.createPkt(ds);
        final PacketInfo info = parser.parsePacket(pkt);
        assertEquals(2, info.getType());
    }

    @Test
    public void testPriorityExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final PacketInfo ds = new PacketInfo();
        ds.setLength(22);
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
        final byte[] pkt = parser.createPkt(ds);
        final PacketInfo info = parser.parsePacket(pkt);
        assertEquals(5, info.getPriority());
    }

    @Test
    public void testModuleExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final PacketInfo ds = new PacketInfo();
        ds.setLength(22);
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
        final byte[] pkt = parser.createPkt(ds);
        final PacketInfo info = parser.parsePacket(pkt);
        assertEquals(9, info.getModule());
    }

    @Test
    public void testConnectionTypeExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final PacketInfo ds = new PacketInfo();
        ds.setLength(22);
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
        final byte[] pkt = parser.createPkt(ds);
        final PacketInfo info = parser.parsePacket(pkt);
        assertEquals(6, info.getConnectionType());
    }

    @Test
    public void testBroadcastExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final PacketInfo ds = new PacketInfo();
        ds.setLength(22);
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
        final byte[] pkt = parser.createPkt(ds);
        final PacketInfo info = parser.parsePacket(pkt);
        assertEquals(1, info.getBroadcast());
    }

    @Test
    public void testIpAddressExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("192.168.1.50");
        final PacketInfo ds = new PacketInfo();
        ds.setLength(22);
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
        final byte[] pkt = parser.createPkt(ds);
        final PacketInfo info = parser.parsePacket(pkt);
        assertEquals(ip, info.getIpAddress());
    }

    @Test
    public void testPortNumExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final int port = 8080;
        final PacketInfo ds = new PacketInfo();
        ds.setLength(22);
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
        final byte[] pkt = parser.createPkt(ds);
        final PacketInfo info = parser.parsePacket(pkt);
        assertEquals(port, info.getPortNum());
    }

    @Test
    public void testMessageIdExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final int messageId = 123456;
        final PacketInfo ds = new PacketInfo();
        ds.setLength(22);
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
        final byte[] pkt = parser.createPkt(ds);
        final PacketInfo info = parser.parsePacket(pkt);
        assertEquals(messageId, info.getMessageId());
    }

    @Test
    public void testChunkNumExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final int chunkNum = 654321;
        final PacketInfo ds = new PacketInfo();
        ds.setLength(22);
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
        final byte[] pkt = parser.createPkt(ds);
        final PacketInfo info = parser.parsePacket(pkt);
        assertEquals(chunkNum, info.getChunkNum());
    }

    @Test
    public void testChunkLengthExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final int chunkLength = 12345678;
        final PacketInfo ds = new PacketInfo();
        ds.setLength(22);
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
        final byte[] pkt = parser.createPkt(ds);
        final PacketInfo info = parser.parsePacket(pkt);
        assertEquals(chunkLength, info.getChunkLength());
    }

    @Test
    public void testPayloadExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final byte[] payload = { 10, 20, 30, 40 };
        final PacketInfo ds = new PacketInfo();
        ds.setLength(26);
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
        final byte[] pkt = parser.createPkt(ds);
        final PacketInfo info = parser.parsePacket(pkt);
        assertArrayEquals(payload, info.getPayload());
    }

    @Test
    public void testPkt() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();

        final int length = 27;
        final int type = 3;
        final int priority = 7;
        final int module = 15;
        final int connectionType = 5;
        final int broadcast = 1;
        final InetAddress ip = InetAddress.getByName("10.0.0.5");
        final int port = 65535;
        final int MessageId = 12345678;
        final int ChunkNum = 87654321;
        final int ChunkLength = 1024;
        final byte[] data = { 1, 2, 3, 4, 5 };

        final PacketInfo ds = new PacketInfo();
        ds.setLength(length);
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

        final byte[] pkt = parser.createPkt(ds);

        final PacketInfo info = parser.parsePacket(pkt);

        assertEquals(length, info.getLength());
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