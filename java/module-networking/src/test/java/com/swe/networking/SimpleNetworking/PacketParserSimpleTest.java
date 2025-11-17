package com.swe.networking.SimpleNetworking;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PacketParserSimpleTest {
    /**
     * Simple test function.
     * @throws UnknownHostException when parsing
     */
    @org.junit.jupiter.api.Test
    public void testTypeExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final byte[] pkt = parser.createPkt(0, 0, 0, 0, ip, 0, new byte[0]);
        assertEquals(0, parser.getType(pkt));
    }

    /**
     * Simple test function.
     * @throws UnknownHostException when parsing
     */
    @org.junit.jupiter.api.Test
    public void testPriorityExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final byte[] pkt = parser.createPkt(5, 0, 0, 0, ip, 0, new byte[0]);
        assertEquals(5, parser.getPriority(pkt));
    }

    /**
     * Simple test function.
     * @throws UnknownHostException when parsing
     */
    @org.junit.jupiter.api.Test
    public void testModuleExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final byte[] pkt = parser.createPkt(0, 9, 0, 0, ip, 0, new byte[0]); // 9 = 1001 (binary)
        assertEquals(9, parser.getModule(pkt));
    }

    /**
     * Simple test function.
     * @throws UnknownHostException when parsing
     */
    @org.junit.jupiter.api.Test
    public void testConnectionTypeExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final byte[] pkt = parser.createPkt(0, 0, 6, 0, ip, 0, new byte[0]);
        assertEquals(6, parser.getConnectionType(pkt));
    }

    /**
     * Simple test function.
     * @throws UnknownHostException when parsing
     */
    @org.junit.jupiter.api.Test
    public void testBroadcastExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final byte[] pkt = parser.createPkt(0, 0, 0, 1, ip, 0, new byte[0]);
        assertEquals(1, parser.getBroadcast(pkt));
    }

    /**
     * Simple test function.
     * @throws UnknownHostException when parsing
     */
    @org.junit.jupiter.api.Test
    public void testIpAddressExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("192.168.1.50");
        final byte[] pkt = parser.createPkt(0, 0, 0, 0, ip, 0, new byte[0]);
        assertEquals(ip, parser.getIpAddress(pkt));
    }

    /**
     * Simple test function.
     * @throws UnknownHostException when parsing
     */
    @org.junit.jupiter.api.Test
    public void testPortNumExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final int port = 8080;
        final byte[] pkt = parser.createPkt(0, 0, 0, 0, ip, port, new byte[0]);
        assertEquals(port, parser.getPortNum(pkt));
    }

    /**
     * Simple test function.
     * @throws UnknownHostException when parsing
     */
    @org.junit.jupiter.api.Test
    public void testPayloadExtraction() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final byte[] payload = { 10, 20, 30, 40 };
        final byte[] pkt = parser.createPkt(0, 0, 0, 0, ip, 0, payload);
        assertArrayEquals(payload, parser.getPayload(pkt));
    }

    /**
     * Simple test function.
     * @throws UnknownHostException when parsing
     */
    @org.junit.jupiter.api.Test
    public void testPkt() throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();

        final int type = 0;
        final int priority = 7;
        final int module = 15;
        final int connectionType = 5;
        final int broadcast = 1;
        final InetAddress ip = InetAddress.getByName("10.0.0.5");
        final int port = 65535;
        final byte[] data = {1, 2, 3, 4, 5 };

        final byte[] pkt = parser.createPkt(priority, module, connectionType, broadcast, ip, port, data);

        assertEquals(type, parser.getType(pkt));
        assertEquals(priority, parser.getPriority(pkt));
        assertEquals(module, parser.getModule(pkt));
        assertEquals(connectionType, parser.getConnectionType(pkt));
        assertEquals(broadcast, parser.getBroadcast(pkt));
        assertEquals(ip, parser.getIpAddress(pkt));
        assertEquals(port, parser.getPortNum(pkt));
        assertArrayEquals(data, parser.getPayload(pkt));
    }
}
