package com.swe.networking;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Classs to split the received data into packets.
 */
public class SplitPackets {

    /**
     * The module name for logging.
     */
    private static final String MODULENAME = "[SPLITPACKETS]";

    /**
     * Singleton design pattern to prevent repeating class instantiations.
     *
     */
    private static SplitPackets splitPackets = null;

    /**
     * Variable to store the buffer size for incomplete buffer.
     */
    private final int bufferSize = 30 * 1024;

    /**
     * Buffer to store the incomplete packets from the data.
     */
    private final ByteBuffer incompleteBuffer = ByteBuffer.allocate(bufferSize);

    /**
     * Variable to store maximuum packet length size.
     */
    private static final int MAX_PACKET_SIZE = 65536;

    private SplitPackets() {
    }

    /**
     * Function to get the statically instantiated class object.
     *
     * @return SplitPackets the statically instantiated class.
     */
    public static SplitPackets getSplitPackets() {
        if (splitPackets == null) {
            NetworkLogger.printInfo(MODULENAME, "Creating new SplitPackets object...");
            splitPackets = new SplitPackets();
        }
        NetworkLogger.printInfo(MODULENAME, "Passing already instantiated SplitPackets object...");
        return splitPackets;
    }

    /**
     * Function to split the packet received.
     *
     * @param data the data to be split
     * @return the list of packets
     */
    public List<byte[]> split(final byte[] data) {
        final List<byte[]> packets = new ArrayList<>();
        final ByteBuffer buffer;

        if (incompleteBuffer.position() > 0) {
            incompleteBuffer.flip();
            final int oldDataLen = incompleteBuffer.remaining();
            NetworkLogger.printInfo(MODULENAME, "Remaining data from previous read: " + oldDataLen + " bytes.");
            final byte[] oldData = new byte[oldDataLen];
            incompleteBuffer.get(oldData);

            final byte[] combined = new byte[oldData.length + data.length];
            NetworkLogger.printInfo(MODULENAME, "Combined length: " + combined.length + " bytes.");
            System.arraycopy(oldData, 0, combined, 0, oldData.length);
            System.arraycopy(data, 0, combined, oldData.length, data.length);
            buffer = ByteBuffer.wrap(combined);
            incompleteBuffer.clear();
        } else {
            buffer = ByteBuffer.wrap(data);
        }
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        NetworkLogger.printInfo(MODULENAME, "Processing buffer of size: " + data.length + " bytes.");
        while (buffer.hasRemaining() && buffer.remaining() > 2) {
            buffer.mark();
            final int len = buffer.getShort();
            NetworkLogger.printInfo(MODULENAME, "Extracted packet length: " + len);
            buffer.reset();
            if (len <= 2 || len > MAX_PACKET_SIZE) {
                NetworkLogger.printWarning(MODULENAME, "Invalid packet length " + len);
            }
            if (buffer.remaining() < len) {
                buffer.reset();
                break;
            }
            final byte[] packet = new byte[len];
            buffer.reset();
            buffer.get(packet);
            packets.add(packet);
        }

        incompleteBuffer.clear();
        if (buffer.hasRemaining()) {
            final int remaining = buffer.remaining();
            incompleteBuffer.put(buffer);
            NetworkLogger.printInfo(MODULENAME, "Carrying over " + remaining + " bytes to next read.");
        }
        return packets;
    }
}
