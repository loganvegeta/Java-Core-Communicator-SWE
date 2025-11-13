package com.swe.networking;

import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// File owned by Vishwaa.

/**
 * Priority Queue with simple Multi-Level Feedback Queue (MLFQ).
 */
public class PriorityQueue {

    /**
     * Variable to store the name of the module.
     */
    private static final String MODULENAME = "[PRIORITY QUEUE]";
    /**
     * The static class object for the priority queue.
     */
    private static PriorityQueue priorityQueue = null;
    /**
     * Number of levels in Multi Level Queue.
     */
    private static final int MLFQ_LEVELS = 3; // 0, 1, 2
    /**
     * Time for each epoch (budget reset).
     */
    private static final int EPOCH_MS = 10;
    /**
     * Total number of packets to be sent in one epoch.
     */
    private static final int TOTAL_BUDGET = 100;
    /**
     * Time difference required for each rotation of MLFQ.
     */
    private static final int ROTATION_TIME = 1000;
    /**
     * Queue for video packet (highest priority).
     */
    private final Deque<byte[]> highestPriorityQueue = new ArrayDeque<>();
    /**
     * Queue for screen share packet (mid-priority).
     */
    private final Deque<byte[]> midPriorityQueue = new ArrayDeque<>();
    /**
     * Multilevel Feedback Queue (MLFQ) for other packets (low priority).
     */
    private final List<Deque<byte[]>> mlfq = new ArrayList<>();
    /**
     * Current bandwidth tokens.
     */
    private final Map<PacketPriority, Integer> currentBudget = new EnumMap<>(PacketPriority.class);
    /**
     * Last time budgets were reset (epoch marker).
     */
    private long lastEpochReset = System.currentTimeMillis();
    /**
     * Last time queues were rotated.
     */
    private long lastRotation = System.currentTimeMillis();
    /**
     * Private variable to store the start time of the Priority Queue.
     */
    private long startTime;
    /**
     * Private Variable to store the number of packets sent.
     */
    private long numPacketsSent;

    /**
     * Creates a priority queue and initializes budgets and queues.
     */
    private PriorityQueue() {
        // System.out.println("Networking][Priority Queue] MLFQ has been created");
        for (int i = 0; i < MLFQ_LEVELS; i++) {
            mlfq.add(new ArrayDeque<>());
        }
        startTime = System.currentTimeMillis();
        numPacketsSent = 0;
        resetBudgets();
    }

    /**
     * Function to return the singleton Priority Queue object.
     *
     * @return the singleton object
     */
    public static PriorityQueue getPriorityQueue() {
        if (priorityQueue == null) {
            priorityQueue = new PriorityQueue();
            NetworkLogger.printInfo(MODULENAME, "Instantiated a new priority Queue...");
            return priorityQueue;
        }
        NetworkLogger.printInfo(MODULENAME, "Passing already instantiated priority Queue...");
        return priorityQueue;
    }

    /**
     * This function returns the current remaining total budget.
     *
     * @return the remaining total budget.
     */
    private int getTotalRemainingBudget() {
        // Sums up all remaining tokens from the HIGHEST,HIGH,and MLFQ buckets.
        return currentBudget.values().stream()
                .mapToInt(Integer::intValue).sum();
    }

    /**
     * Empties the priority queue for each test.
     */
    public void clear() {
        highestPriorityQueue.clear();
        midPriorityQueue.clear();
        for (Deque<byte[]> level : mlfq) {
            level.clear();
        }
        resetBudgets();
    }

    /**
     * This function says whether there are any packets to be sent.
     *
     * @return true if there are packets left.
     */
    public boolean isEmpty() {
        // Checking the highest and mid-priority queue
        if (!highestPriorityQueue.isEmpty() || !midPriorityQueue.isEmpty()) {
            return false;
        }

        // Check all levels of the MLFQ
        for (Deque<byte[]> level : mlfq) {
            if (!level.isEmpty()) {
                return false; // If any MLFQ level has packets, it's NOT empty
            }
        }

        // If everything is empty
        return true;
    }

    /**
     * Resets budgets at the beginning of each epoch.
     */
    private void resetBudgets() {
        for (PacketPriority p : PacketPriority.values()) {
            final int tokens = (TOTAL_BUDGET * p.getShare()) / TOTAL_BUDGET;
            currentBudget.put(p, tokens);
        }
        NetworkLogger.printInfo(MODULENAME, "Reset Initiated...");
        lastEpochReset = System.currentTimeMillis();
    }

    /**
     * Rotates MLFQ levels every 1000 ms. Level 0 → Level 1, Level 1 → Level 2,
     * Level 2 → Level 0(recycled)
     */
    public void rotateQueues() {
        final long now = System.currentTimeMillis();
        if (now - lastRotation >= ROTATION_TIME) {
            NetworkLogger.printInfo(MODULENAME, "Rotating MLFQ levels...");
            final Deque<byte[]> level2 = mlfq.get(2);
            final Deque<byte[]> level1 = mlfq.get(1);
            final Deque<byte[]> level0 = mlfq.get(0);

            final Deque<byte[]> recycled = new ArrayDeque<>(level2);

            // Rotate down
            mlfq.set(2, level1);
            mlfq.set(1, level0);

            // Wrap old level3 back into level0
            mlfq.set(0, recycled);

            lastRotation = now;
        }
    }

    /**
     * This function gives the approx throughput of the Priority Queue. This
     * assumes that there are enough number of packets.
     *
     * @return The minimum throughput of the Priority Queue
     */
    public long getThroughput() {

        final long currTime = System.currentTimeMillis();
        final long timeTaken = currTime - startTime;
        final int packetLength = 1000;

        return (packetLength * numPacketsSent) / timeTaken;
    }

    /**
     * Adds a packet to the appropriate queue based on its priority.
     *
     * @param data the packet payload
     */
    public synchronized void addPacket(final byte[] data) throws UnknownHostException {
        final PacketParser parser = PacketParser.getPacketParser();
        final PacketInfo info = parser.parsePacket(data);
        final int priorityLevel = info.getPriority();
        final PacketPriority priority = PacketPriority.fromLevel(priorityLevel);

        switch (priority) {
            case ZERO, ONE, TWO:
                highestPriorityQueue.add(data);
                NetworkLogger.printInfo(MODULENAME, "Packet added to the Highest priority queue");
                break;
            case THREE, FOUR, FIVE, SIX:
                midPriorityQueue.add(data);
                NetworkLogger.printInfo(MODULENAME, "Packet added to mid priority queue");
                break;
            default:
                // All low-priority packets start at level 0
                mlfq.get(0).add(data);
                NetworkLogger.printInfo(MODULENAME, "Packet added to MLFQ level 0");
                break;
        }
    }

    /**
     * Processes and sends a packet from the Highest Priority Queue (P1/ZERO).
     *
     * @return The packet data, or null.
     */
    private byte[] processHighestPriority() {
        if (!highestPriorityQueue.isEmpty()
                && currentBudget.get(PacketPriority.ZERO) > 0) {
            currentBudget.put(PacketPriority.ZERO,
                    currentBudget.get(PacketPriority.ZERO) - 1);
            NetworkLogger.printInfo(MODULENAME, "Highest Priority Packet sent");
            return highestPriorityQueue.pollFirst();
        }
        return null;
    }

    /**
     * Processes and sends a packet from the Mid-Priority Queue (P2/ONE).
     * Work-conserving: uses P2's budget, then P1's unused budget.
     *
     * @return The packet data, or null.
     */
    private byte[] processMidPriority() {
        final int p2Current = currentBudget.get(PacketPriority.ONE);
        final int p1Current = currentBudget.get(PacketPriority.ZERO);
        final int totalP2Budget = p2Current + p1Current;

        if (!midPriorityQueue.isEmpty() && totalP2Budget > 0) {
            // Check current tokens again

            if (p2Current > 0) {
                // Use P2's own budget
                currentBudget.put(PacketPriority.ONE, p2Current - 1);
                NetworkLogger.printInfo(MODULENAME, "Mid-priority sent from p2 budget");
            } else if (p1Current > 0) {
                // Use P1's unused budget
                currentBudget.put(PacketPriority.ZERO, p1Current - 1);
                NetworkLogger.printInfo(MODULENAME, "Mid-priority sent from p1 budget");
            }
            return midPriorityQueue.pollFirst();
        }
        return null;
    }

    /**
     * Processes and sends a packet from the Low Priority (MLFQ)Queues(P3/TWO).
     * Work-conserving: uses P3, then P2, then P1 budget.
     *
     * @return The packet data, or null.
     */
    private byte[] processLowPriority() {
        final int p3Current = currentBudget.get(PacketPriority.TWO);
        final int p2Current = currentBudget.get(PacketPriority.ONE);
        final int p1Current = currentBudget.get(PacketPriority.ZERO);
        final int totalP3Budget = p3Current + p2Current + p1Current;

        if (totalP3Budget > 0) {
            for (int i = 0; i < mlfq.size(); i++) {
                final Deque<byte[]> q = mlfq.get(i);
                if (!q.isEmpty()) {

                    // Decrement the budget in order: P3 -> P2 -> P1
                    if (p3Current > 0) {
                        currentBudget.put(PacketPriority.TWO, p3Current - 1);
                        NetworkLogger.printInfo(MODULENAME, "Low Priority Packet sent from p3 budget");
                    } else if (p2Current > 0) {
                        currentBudget.put(PacketPriority.ONE, p2Current - 1);
                        NetworkLogger.printInfo(MODULENAME, "Low Priority Packet sent from p2 budget");
                    } else if (p1Current > 0) { // Use P1's budget
                        currentBudget.put(PacketPriority.ZERO,
                                p1Current - 1);
                        NetworkLogger.printInfo(MODULENAME, "Low Priority Packet sent from p1 budget");
                    } else {
                        // This case should be covered by
                        // the totalP3Budget > 0 check,
                        // but if budgets were zeroed between
                        // the check and here, skip.
                        continue;
                    }

                    NetworkLogger.printInfo(MODULENAME, "Low Priority Packet sent from MLFQ level " + i);
                    return q.pollFirst();
                }
            }
        }
        return null; // nothing available or budget exhausted
    }

    /**
     * A private function which gets the packet from the queue. The packet could
     * be null, and it signals to wait.
     *
     * @return The Packet data or null
     */
    private byte[] trySendNext() {
        // 1. Reset budgets every epoch
        if (getTotalRemainingBudget() <= 0) {
            resetBudgets();
        }

        final long now = System.currentTimeMillis();

        if (now - lastEpochReset >= EPOCH_MS) {
            resetBudgets();
        }

        // 2. Rotate MLFQ queues
        rotateQueues();

        // 3. Process priorities in order: P1 > P2 > P3
        // Highest Priority (P1/ZERO)
        byte[] packet = processHighestPriority();
        if (packet != null) {
            return packet;
        }

        // Mid-Priority (P2/ONE)
        packet = processMidPriority();
        if (packet != null) {
            return packet;
        }

        // Low Priority (P3/TWO - MLFQ)
        packet = processLowPriority();
        if (packet != null) {
            return packet;
        }
        return null;
    }

    /**
     * Retrieves the next packet to process.
     *
     * @return the next packet's data, or null if none available
     */
    public synchronized byte[] nextPacket() {
        byte[] packet;
        final int maxRetryCount = 10;
        int retryCount = 0;

        while (true) {
            // If the queue is empty it return null.
            if (isEmpty()) {
                return null;
            }

            // Gets the Packet from tryNextSend.
            packet = trySendNext();
            if (packet != null) {
                numPacketsSent++;
                return packet;
            }

            // If the packet is null it makes the thread wait and they retry.
            try {
                NetworkLogger.printInfo(MODULENAME, "No packets has been received, Thread going to sleep");
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
            }

            retryCount++;
            // optional safety escape
            if (retryCount > maxRetryCount) {
                NetworkLogger.printInfo(MODULENAME, "Max number of retires has been reached, Returning null.");
                return null;
            }
        }

    }

    /**
     * Enum representing packet priorities and corresponding budget shares.
     */
    public enum PacketPriority {
        /**
         * Highest priority packet (50% budget share).
         */
        ZERO(0, 50),
        /**
         * High priority packet (30% budget share).
         */
        ONE(1, 30),
        /**
         * Low priority packets handled by MLFQ (20% budget share).
         */
        TWO(2, 20),
        /**
         * Future extension: low priority.
         */
        THREE(3, 0),
        /**
         * Future extension: very low priority.
         */
        FOUR(4, 0),
        /**
         * Future extension: bulk priority.
         */
        FIVE(5, 0),
        /**
         * Future extension: background priority.
         */
        SIX(6, 0),
        /**
         * Future extension: lowest priority.
         */
        SEVEN(7, 0);

        /**
         * Priority level (1–8).
         */
        private final int priorityLevel;

        /**
         * Percentage share of total budget.
         */
        private final int budgetShare;

        /**
         * Constructor for PacketPriority.
         *
         * @param level priority level (0-7)
         * @param share percentage of total budget for this priority
         */
        PacketPriority(final int level, final int share) {
            this.priorityLevel = level;
            this.budgetShare = share;
        }

        /**
         * Gets the budget share for this priority.
         *
         * @return percentage of total budget
         */
        public int getShare() {
            return budgetShare;
        }

        /**
         * Returns the PacketPriority corresponding to a given level.
         *
         * @param searchLevel the level to look up
         * @return the PacketPriority enum
         * @throws IllegalArgumentException if level is invalid
         */
        public static PacketPriority fromLevel(final int searchLevel) {
            for (PacketPriority p : values()) {
                if (p.priorityLevel == searchLevel) {
                    return p;
                }
            }
            throw new IllegalArgumentException(
                    "Invalid priority level: " + searchLevel);
        }
    }
}
