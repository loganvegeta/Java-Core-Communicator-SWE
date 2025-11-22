package com.swe.networking.SimpleNetworking;

import java.net.InetAddress;
import java.net.UnknownHostException;

/* Parser for the packets.
The structure of the packet is given below
- Type              : 2bits
- Priority          : 3bits
- Module            : 4bits
- Connection Type   : 3bits
- Broadcast         : 1bit
- empty             : 3bits ( for future use )
- IPv4 addr         : 32bits
- port num          : 16bits


0                             1
0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5  6
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
|Type |Priority|   Module  |Con Type|BC|  empty |
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
|                  IPv4 Address                 |
|                                               |
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
|                 port number                   |
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
| Payload....
+--+--+--+--+-
*/

/**
 * Class for packet parser.
 */
public class PacketParser {

    /** Shift to extract the TYPE. */
    private static final int TYPE_SHIFT = 6;
    /** Mask for the TYPE field (2 bits). */
    private static final int TYPE_MASK = 0b11;
    /** Default TYPE value (2-bit field). */
    private static final int TYPE_DEFAULT = 0b00;

    /** Shift to extract the PRIORITY field from byte 0. */
    private static final int PRIORITY_SHIFT = 3;
    /** Mask for the PRIORITY field (3 bits). */
    private static final int PRIORITY_MASK = 0b111;

    /** Mask for the module high bits stored in byte 0 (3 bits). */
    private static final int MODULE_HIGH_MASK = 0b111;
    /** bits to shift the high portion for module value. */
    private static final int MODULE_HIGH_SHIFT = 1;
    /** Mask for byte 0 from  full module value (bits 3..1). */
    private static final int MODULE_HIGH_BUILD_MASK = 0b1110;
    /** Mask for the module low bit stored in byte 1 (1 bit). */
    private static final int MODULE_LOW_MASK = 0b1;
    /** Bit position (0..7) for the module low bit inside byte 1. */
    private static final int MODULE_LOW_SHIFT = 7;

    /** Shift to extract the CONNECTION_TYPE. */
    private static final int CONNECTION_SHIFT = 4;
    /** Mask for the CONNECTION_TYPE field (3 bits). */
    private static final int CONNECTION_MASK = 0b111;

    /** Shift to extract the BROADCAST flag. */
    private static final int BROADCAST_SHIFT = 3;
    /** Mask for the BROADCAST flag (1 bit). */
    private static final int BROADCAST_MASK = 0b1;

    /** Offset for IPv4 address. */
    private static final int IP_OFFSET = 2;
    /** Number of bytes in an IPv4 address. */
    private static final int IP_LENGTH = 4;

    /** Offset for port. */
    private static final int PORT_OFFSET = 6;
    /** number of header bytes before payload. */
    private static final int HEADER_LENGTH = 8;
    /** Mask for getting unsigned value (0..255). */
    private static final int BYTE_MASK = 0xFF;
    /** bits to shift the high port. */
    private static final int PORT_SHIFT = 8;

    /** Static class object for packet parser. */
    private static PacketParser parser = null;

    /** Private constructor class. */
    private PacketParser() {

    }

    /**
     * Function to get the packetparser.
     *
     * @return the singleton object.
     */
    public static PacketParser getPacketParser() {
        if (parser == null) {
            parser = new PacketParser();
        }
        return parser;
    }

    /**
     * Function to get the type of packet.
     *
     * @param pkt the packet
     * @return the type
     */
    public int getType(final byte[] pkt) {
        return (pkt[0] >> TYPE_SHIFT) & TYPE_MASK;
    }

    /**
     * Function to get the priority of packet.
     *
     * @param pkt the packet
     * @return the priority
     */
    public int getPriority(final byte[] pkt) {
        return (pkt[0] >> PRIORITY_SHIFT) & PRIORITY_MASK;
    }

    /**
     * Function to get the module of packet.
     *
     * @param pkt the packet
     * @return the module
     */
    public int getModule(final byte[] pkt) {
        return ((pkt[0] & MODULE_HIGH_MASK) << MODULE_HIGH_SHIFT)
                | ((pkt[1] >> MODULE_LOW_SHIFT) & MODULE_LOW_MASK);
    }

    /**
     * Function to get the connection type of packet.
     *
     * @param pkt the packet
     * @return the connection type
     */
    public int getConnectionType(final byte[] pkt) {
        return (pkt[1] >> CONNECTION_SHIFT) & CONNECTION_MASK;
    }

    /**
     * Function to get the broadcast of packet.
     *
     * @param pkt the packet
     * @return the broadcast
     */
    public int getBroadcast(final byte[] pkt) {
        return (pkt[1] >> BROADCAST_SHIFT) & BROADCAST_MASK;
    }

    /**
     * Function to get the ip address of packet.
     *
     * @param pkt the packet
     * @return the ip address
     */
    public InetAddress getIpAddress(final byte[] pkt) throws UnknownHostException {
        final byte[] ipBytes = new byte[IP_LENGTH];
        for (int i = 0; i < IP_LENGTH; i++) {
            ipBytes[i] = (byte) (pkt[IP_OFFSET + i] & BYTE_MASK);
        }
        return InetAddress.getByAddress(ipBytes);
    }

    /**
     * Function to get the port of packet.
     *
     * @param pkt the packet
     * @return the port
     */
    public int getPortNum(final byte[] pkt) {
        return ((pkt[PORT_OFFSET] & BYTE_MASK) << PORT_SHIFT)
                | (pkt[PORT_OFFSET + 1] & BYTE_MASK);
    }

    /**
     * Function to get the payload of packet.
     *
     * @param pkt the packet
     * @return the payload
     */
    public byte[] getPayload(final byte[] pkt) {
        final byte[] payload = new byte[pkt.length - HEADER_LENGTH];
        System.arraycopy(pkt, HEADER_LENGTH, payload, 0, payload.length);
        return payload;
    }

    /**
     * Function to create the packet.
     *
     * @param priority       the priority of packet
     * @param module         the module of packet
     * @param connectionType the connection type of packet
     * @param broadCast      the broadcast of packet
     * @param ipAddr         the ip address of packet
     * @param portNum        the port of packet
     * @param data           the payload of packet
     * @return the packet
     */
    public byte[] createPkt(final int priority, final int module,
            final int connectionType, final int broadCast, final InetAddress ipAddr,
            final int portNum, final byte[] data) {
        final byte[] pkt = new byte[data.length + HEADER_LENGTH];
        // Byte 0 = Type (2b) + Priority (3b) + Module[3:1] (3b)
        pkt[0] = (byte) ((TYPE_DEFAULT << TYPE_SHIFT)
            | ((priority & PRIORITY_MASK) << PRIORITY_SHIFT)
            | ((module & MODULE_HIGH_BUILD_MASK) >> MODULE_HIGH_SHIFT));

        // Byte 1 = Module[0] (1b) + connectionType (3b) + broadCast (1b) + Empty (3b)
        pkt[1] = (byte) (((module & MODULE_LOW_MASK) << MODULE_LOW_SHIFT)
            | ((connectionType & CONNECTION_MASK) << CONNECTION_SHIFT)
            | ((broadCast & BROADCAST_MASK) << BROADCAST_SHIFT));

        // Byte 2..5
        final byte[] ipBytes = ipAddr.getAddress();
        System.arraycopy(ipBytes, 0, pkt, IP_OFFSET, IP_LENGTH);

        // Byte 6,7
        pkt[PORT_OFFSET] = (byte) (portNum >> PORT_SHIFT);
        pkt[PORT_OFFSET + 1] = (byte) (portNum & BYTE_MASK);

        System.arraycopy(data, 0, pkt, HEADER_LENGTH, data.length);
        return pkt;
    }
}