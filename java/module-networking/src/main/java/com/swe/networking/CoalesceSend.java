package com.swe.networking;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The class implementing coalescing before data is sent.
 */
public class CoalesceSend {

    /**
     * Variable to store the name of the module.
     */
    private static final String MODULENAME = "[COALESCESEND]";

    /**
     * Map which stores a list of packets corresponding to the same destination.
     */
    private final Map<String, CoalescedPacket> coalescedPackets;

    /**
     * Maximum size of each coalesced packet in bytes.
     */
    private final int maxSize = 2048; // 2 KB

    /**
     * Maximum time before timeout.
     */
    private final int maxTime = 10; // 10 ms

    public CoalesceSend() {
        this.coalescedPackets = new HashMap<>();
        NetworkLogger.printInfo(MODULENAME, "CoalesceSend initialized...");
    }

    /**
     * Adds packets to coalescing lists based on their destination.
     *
     * @param data The payload of the packet.
     * @param destIP The IP of the destination.
     * @param destPort The port of the destination.
     * @param module The module where the data is to be sent.
     */
    public void handlePacket(final byte[] data, final InetAddress destIP, final int destPort, final byte module) {
        final String destination = destIP.getHostAddress() + ":" + destPort;
        NetworkLogger.printInfo(MODULENAME, "Handling packet for destination: " + destination);

        CoalescedPacket coalescedPacket = coalescedPackets.get(destination);
        if (coalescedPacket == null) {
            NetworkLogger.printInfo(MODULENAME, "No existing coalesced packet for "
                    + destination + ". Creating new one.");
            coalescedPacket = new CoalescedPacket();
            coalescedPackets.put(destination, coalescedPacket);
        }

        final int packetSize = 4 + 1 + data.length; // packetSize + module + data

        final ByteBuffer buffer = ByteBuffer.allocate(packetSize);
        buffer.putInt(packetSize); // 4 bytes
        buffer.put(module); // 1 byte
        buffer.put(data); // data.length bytes

        final byte[] packet = new byte[buffer.position()];
        buffer.flip();
        buffer.get(packet);

        coalescedPacket.addToQueue(packet);
        NetworkLogger.printInfo(MODULENAME, "Added packet of size " + packet.length + " to queue for " + destination);

        if (coalescedPacket.getTotalSize() >= maxSize) {
            NetworkLogger.printInfo(MODULENAME, "Max size reached for " + destination + ". Sending coalesced packet.");
            sendCoalescedPacket(destination, coalescedPacket);
            coalescedPackets.remove(destination);
        }
    }

    private void sendCoalescedPacket(final String destination, final CoalescedPacket coalescedPacket) {
        final String ip = destination.split(":")[0];
        final int port = Integer.parseInt(destination.split(":")[1]);
        try {
            final InetAddress destIP = InetAddress.getByName(ip);
            NetworkLogger.printInfo(MODULENAME, "Sending coalesced packet to " + destination);

            final ByteBuffer buffer = ByteBuffer.allocate(coalescedPacket.getTotalSize());

            while (coalescedPacket.getTotalSize() > 0) {
                buffer.put(coalescedPacket.getQueueHead());
            }

            final byte[] payload = new byte[buffer.position()];
            buffer.flip();
            buffer.get(payload);

            NetworkLogger.printInfo(MODULENAME, "Coalesced packet of size "
                    + payload.length + " sent to " + destination);
            // Todo: Send the module: networking, destIP, port and payload to the chunk
            // manager.
        } catch (UnknownHostException e) {
            NetworkLogger.printError(MODULENAME, "Error sending coalesced packet: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Function to check timeout of each coalesced packet i.e., coalescedPacket.
     * It should be invoked by a scheduled executor or timer.
     */
    public void checkTimeout() {
        final long now = System.currentTimeMillis();

        final Iterator<Map.Entry<String, CoalescedPacket>> iterator = coalescedPackets.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<String, CoalescedPacket> entry = iterator.next();

            final CoalescedPacket coalescedPacket = entry.getValue();

            if (now - coalescedPacket.getStartTime() >= maxTime) {
                NetworkLogger.printInfo(MODULENAME, "Timeout reached for "
                        + entry.getKey() + ". Sending coalesced packet.");
                sendCoalescedPacket(entry.getKey(), coalescedPacket);
                iterator.remove();
            }
        }
    }
}
