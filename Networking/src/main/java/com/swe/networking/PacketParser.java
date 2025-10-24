package com.swe.networking;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

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
- Message Id        : 32bits
- Chunk Num         : 32bits
- Chunk Length      : 32bits
- Payload           : variable length


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
|                  Message Id                   |
|                                               |
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
|                  Chunk Number                 |
|                                               |
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
|                  Chunk Length                 |
|                                               |
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
| Payload....                                   
+--+--+--+--+--+
*/

/**
 * The Packet Parser class to parse packets.
 */
public class PacketParser {

    /**
     * Singleton instance of PacketParser.
     */
    private static PacketParser parser = null;
    
    /**
     * Size of the packet header in bytes.
     */
    private static final int HEADER_SIZE = 20;
    /** Number of bytes in an IPv4 address. */
    private static final int LEN_IP = 4;
    /** Number of bytes used to store a port number. */
    private static final int LEN_PORT = 2;
    /** Number of bytes used to store the message id. */
    private static final int LEN_MESSAGE_ID = 4;
    /** Number of bytes used to store the chunk number. */
    private static final int LEN_CHUNK_NUM = 4;
    /** Number of bytes used to store the chunk length. */
    private static final int LEN_CHUNK_LENGTH = 4;
    // Offsets into the packet
    /** Offset of the flags (type/priority/module bits) byte. */
    private static final int OFF_FLAGS = 0;
    /** Offset of the IPv4 address field. */
    private static final int OFF_IP = 2;
    /** Offset of the port field. */
    private static final int OFF_PORT = 6;
    /** Offset of the message id field. */
    private static final int OFF_MESSAGE_ID = 8;
    /** Offset of the chunk number field. */
    private static final int OFF_CHUNK_NUM = 12;
    /** Offset of the chunk length field. */
    private static final int OFF_CHUNK_LENGTH = 16;

    /** Bitfield sizes within the first two bytes. */
    /** 
     * no of bits in byte 0 for type.
     */
    private static final int BITS_TYPE = 2;
    /** no of bits in byte 0 for priority. */
    private static final int BITS_PRIORITY = 3;
    /**
     * no of bits in byte 0 for module.
     */
    private static final int BITS_MODULE = 4;
    /** no of bits in byte 1 for connection type. */
    private static final int BITS_CONNECTION_TYPE = 3;
    /** no of bits in byte 1 for broadcast. */
    private static final int BITS_BROADCAST = 1;
    /** no of bits in byte 1 for reserved. */
    private static final int BITS_EMPTY = 3;

    /** Bit mask for the type field. */
    private static final int MASK_TYPE = (1 << BITS_TYPE) - 1; // 0b11
    /** Bit mask for the priority field. */
    private static final int MASK_PRIORITY = (1 << BITS_PRIORITY) - 1; // 0b111
    /** Bit mask for the module field's lower 3 bits. */
    private static final int MASK_MODULE_LOWER = (1 << (BITS_MODULE - 1)) - 1; // 0b111
    /** Bit mask for the module field's upper bit. */
    private static final int MASK_MODULE_UPPER = 1; // single bit in byte1 (MSB)
    /** Bit mask for the connection type field. */
    private static final int MASK_CONNECTION_TYPE = (1 << BITS_CONNECTION_TYPE) - 1; // 0b111
    /** Bit mask for the broadcast field. */
    private static final int MASK_BROADCAST = 1;

    /** Shift right amount to extract the type field from byte0. */
    private static final int SHIFT_TYPE = 8 - BITS_TYPE; 
    /** Shift right amount to extract the priority field from byte0. */
    private static final int SHIFT_PRIORITY = 8 - BITS_TYPE - BITS_PRIORITY;
    /** Shift right amount to extract the connection type field from byte1. */
    private static final int SHIFT_CONNECTION_TYPE = BITS_EMPTY + BITS_BROADCAST;
    /** Shift right amount to extract the broadcast field from byte1. */
    private static final int SHIFT_BROADCAST = BITS_EMPTY;
    /** Shift right amount to extract the module's upper bit from byte1. */
    private static final int SHIFT_MODULE_UPPER = 7;

    /**
     * Private constructor.
     */
    private PacketParser() { }

    /**
     * Get the singleton instance of PacketParser.
     *
     * @return PacketParser the singleton instance
     */
    public static PacketParser getPacketParser() {
        if (parser == null) {
            parser = new PacketParser();
        }
        return parser;
    }

    /**
     * Parse a raw packet byte array into a PacketInfo instance.
     *
     * @param pkt raw packet bytes
     * @return PacketInfo populated from the packet
     * @throws UnknownHostException if the IP address is invalid
     */
    public PacketInfo parsePacket(final byte[] pkt) throws UnknownHostException {
        final PacketInfo info = new PacketInfo();

        final int type = (pkt[OFF_FLAGS] >> SHIFT_TYPE) & MASK_TYPE;
        final int priority = (pkt[OFF_FLAGS] >> SHIFT_PRIORITY) & MASK_PRIORITY;

        final int lower = pkt[OFF_FLAGS] & MASK_MODULE_LOWER; // lower 3 bits in byte0
        final int upper = (pkt[OFF_FLAGS + 1] >> SHIFT_MODULE_UPPER) & MASK_MODULE_UPPER; // single bit in byte1
        final int module = (lower << 1) | upper;

        final int connectionType = (pkt[OFF_FLAGS + 1] >> SHIFT_CONNECTION_TYPE) & MASK_CONNECTION_TYPE;
        final int broadcast = (pkt[OFF_FLAGS + 1] >> SHIFT_BROADCAST) & MASK_BROADCAST;

        final byte[] ipBytes = Arrays.copyOfRange(pkt, OFF_IP, OFF_IP + LEN_IP);
        final InetAddress ip = InetAddress.getByAddress(ipBytes);

        final ByteBuffer portBb = ByteBuffer.wrap(pkt, OFF_PORT, LEN_PORT);
        final int port = Short.toUnsignedInt(portBb.getShort());

        final ByteBuffer midBb = ByteBuffer.wrap(pkt, OFF_MESSAGE_ID, LEN_MESSAGE_ID);
        final int messageId = midBb.getInt();

        final ByteBuffer cnumBb = ByteBuffer.wrap(pkt, OFF_CHUNK_NUM, LEN_CHUNK_NUM);
        final int chunkNum = cnumBb.getInt();

        final ByteBuffer clenBb = ByteBuffer.wrap(pkt, OFF_CHUNK_LENGTH, LEN_CHUNK_LENGTH);
        final int chunkLength = clenBb.getInt();

        final byte[] payload = Arrays.copyOfRange(pkt, HEADER_SIZE, pkt.length);

        info.setType(type);
        info.setPriority(priority);
        info.setModule(module);
        info.setConnectionType(connectionType);
        info.setBroadcast(broadcast);
        info.setIpAddress(ip);
        info.setPortNum(port);
        info.setMessageId(messageId);
        info.setChunkNum(chunkNum);
        info.setChunkLength(chunkLength);
        info.setPayload(payload);

        return info;
    }

    /**
     * Create a packet from a PacketInfo instance. The Packet Info holds
     * the packet header attributes and payload data.
     *
     * @param ds the packet info
     * @return the constructed packet as a byte array
     */
    public byte[] createPkt(final PacketInfo ds) {
        final byte[] data = ds.getPayload();
        final byte[] pkt = new byte[HEADER_SIZE + data.length];
        final ByteBuffer bb = ByteBuffer.wrap(pkt);

        //  byte 0 and byte 1 from the bitfield components
        final int typePart = (ds.getType() & MASK_TYPE) << SHIFT_TYPE;
        final int priorityPart = (ds.getPriority() & MASK_PRIORITY) << SHIFT_PRIORITY;
        final int moduleFull = ds.getModule() & ((1 << BITS_MODULE) - 1);

        final int moduleLowerPacked = (moduleFull & (MASK_MODULE_LOWER << 1)) >> 1; // take bits 3..1 and shift down
        final int moduleUpper = moduleFull & MASK_MODULE_UPPER; // LSB

        final byte byte0 = (byte) (typePart | priorityPart | moduleLowerPacked);
        final byte byte1 = (byte) ((moduleUpper << SHIFT_MODULE_UPPER)
                    | ((ds.getConnectionType() & MASK_CONNECTION_TYPE) << SHIFT_CONNECTION_TYPE)
                    | ((ds.getBroadcast() & MASK_BROADCAST) << SHIFT_BROADCAST));

        bb.put(byte0);
        bb.put(byte1);

        // Bytes 2–5: IP
        bb.put(ds.getIpAddress().getAddress());

        // Bytes 6–7: port
        bb.putShort((short) ds.getPortNum());

        // Bytes 8–11: messageId
        bb.putInt(ds.getMessageId());

        // Bytes 12–15: chunkNum
        bb.putInt(ds.getChunkNum());

        // Bytes 16–19: chunkLength
        bb.putInt(ds.getChunkLength());

        // Bytes 20+: payload
        bb.put(data);

        return pkt;
    }
}