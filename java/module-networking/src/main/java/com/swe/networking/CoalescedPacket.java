package com.swe.networking;

import java.util.LinkedList;
import java.util.Queue;

class CoalescedPacket {

    /**
     * Variable to store the name of the module.
     */
    private static final String MODULENAME = "[COALESCEDPACKET]";

    /**
     * Queue storing packets to be coalesced.
     */
    private final Queue<byte[]> queue = new LinkedList<byte[]>();
    /**
     * Current total size of the queue in bytes.
     */
    private int totalSize = 0;
    /**
     * The time at which the coalescedPacket is created.
     */
    private long startTime;

    CoalescedPacket() {
        NetworkLogger.printInfo(MODULENAME, "New coalesced packet created.");
    }

    public void addToQueue(final byte[] packet) {
        if (packet == null || packet.length == 0) {
            NetworkLogger.printInfo(MODULENAME, "Attempted to add null or empty packet to queue. Ignoring.");
            return;
        }

        if (totalSize == 0) {
            this.startTime = System.currentTimeMillis();
            NetworkLogger.printInfo(MODULENAME, "Setting start time for coalesced packet: " + startTime);
        }

        queue.add(packet);
        totalSize += packet.length;
        NetworkLogger.printInfo(MODULENAME, "Packet of size "
                + packet.length + " added to queue. New total size: " + totalSize);
    }

    public long getStartTime() {
        return this.startTime;
    }

    public int getTotalSize() {
        return this.totalSize;
    }

    public byte[] getQueueHead() {
        final byte[] head = this.queue.poll();
        if (head != null) {
            this.totalSize -= head.length;
            NetworkLogger.printInfo(MODULENAME, "Packet of size "
                    + head.length + " retrieved from queue. New total size: " + totalSize);
        } else {
            NetworkLogger.printInfo(MODULENAME, "Attempted to retrieve packet from empty queue.");
        }
        return head;
    }
}
