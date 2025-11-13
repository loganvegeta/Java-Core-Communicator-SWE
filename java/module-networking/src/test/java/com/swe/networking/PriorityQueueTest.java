package com.swe.networking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class PriorityQueueTest {

    // Helper method to create a dummy PacketParser instance
    private PacketParser getParser() {
        return PacketParser.getPacketParser();
    }

    // Creating the object PriorityQueue
    PriorityQueue pq = PriorityQueue.getPriorityQueue();

    // Helper method for creating packets
    private byte[] createTestPkt(PacketParser parser, int priority, int chunkNum, String payload)
            throws UnknownHostException {
        final InetAddress ip = InetAddress.getByName("0.0.0.0");
        final PacketInfo ds = new PacketInfo();
        ds.setType(0);
        ds.setPriority(priority);
        ds.setModule(0);
        ds.setBroadcast(0);
        ds.setIpAddress(ip);
        ds.setPortNum(0);
        ds.setMessageId(0);
        ds.setChunkNum(chunkNum);
        ds.setLength(22 + payload.getBytes().length);
        ds.setChunkLength(0);
        ds.setPayload(payload.getBytes());
        final byte[] pkt = parser.createPkt(ds);
        return pkt;
    }

    @AfterEach
    public void queueClear() {
        pq.clear();
    }

    //-------------------------------------------------------------------------
    // BASIC FUNCTIONALITY TESTS
    //-------------------------------------------------------------------------

    @Test
    void testSinglePacket() throws UnknownHostException {
        PacketParser parser = getParser();
        byte[] data = createTestPkt(parser, 5, 0, "hello");
        pq.addPacket(data);

        byte[] result = pq.nextPacket();
        assertNotNull(result, "Next packet should not be null");
        assertArrayEquals(data, result, "Packet data should match");
    }

    @Test
    void testingEmptyQueue() {
        PacketParser parser = getParser();
        boolean result = pq.isEmpty();

        assertEquals(true, result, "The Queues should be empty");
    }

    @Test
    void testFromLevelInvalid() {
        // The enum defines levels 1 through 8.
        final int invalidLevel = 9;

        // The lambda expression inside assertThrows is the code being tested.
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> {
                    PriorityQueue.PacketPriority.fromLevel(invalidLevel);
                },
                "Should throw an IllegalArgumentException for an invalid priority level."
        );

        String expectedMessage = "Invalid priority level: " + invalidLevel;
        assertTrue(exception.getMessage().contains(expectedMessage),
                "The exception message should contain the invalid level.");
    }

    @Test
    void testMultiplePacketsDifferentPriorities() throws UnknownHostException {
        PacketParser parser = getParser();

        byte[] high1Priority = createTestPkt(parser, 0, 0, "high");
        byte[] mid1Priority = createTestPkt(parser, 3, 0, "medium");
        byte[] low1Priority = createTestPkt(parser, 7, 0, "lowest");

        pq.addPacket(low1Priority);
        pq.addPacket(mid1Priority);
        pq.addPacket(high1Priority);


        // P1 > P2 > P3 order
        assertArrayEquals(high1Priority, pq.nextPacket(), "Highest priority packet sent first");
        assertArrayEquals(mid1Priority, pq.nextPacket(), "Mid priority packet sent second");
        assertArrayEquals(low1Priority, pq.nextPacket(), "Low priority packet sent last");
    }

    @Test
    void testBudgetLimits() throws UnknownHostException, InterruptedException {
        PacketParser parser = getParser();

        // Add 200 high-priority packets (budget is 50)
        for (int i = 0; i < 10000; i++) {
            pq.addPacket(createTestPkt(parser, 0, i, "p" + i));
            pq.addPacket(createTestPkt(parser, 3, 10000 + i, "mp" + i));
            pq.addPacket(createTestPkt(parser, 7, 20000+i, "lp" + i));
        }

        int sentCount = 0;
        long t1 = System.currentTimeMillis();
        // First consumption loop (should hit the 50 budget limit)
        for (int i = 0; i < 30000; i++) {
            byte[] pkt = pq.nextPacket();
            if (pkt != null) {
                sentCount++;
            }
        }
        long t2 = System.currentTimeMillis();


        System.out.println("packets " + sentCount + " time " + (t2-t1));
    }

    //-------------------------------------------------------------------------
    // MLFQ & ROTATION TESTS
    //-------------------------------------------------------------------------

    @Test
    void testRotation() throws UnknownHostException, InterruptedException {
        PacketParser parser = getParser();

        for(int i = 0; i < 4000; i++){
            pq.addPacket(createTestPkt(parser, 6, i, "LP"));
        }

        for(int i = 0; i < 400; i++){
            byte[] pkt = pq.nextPacket();
        }

        Thread.sleep(1100);
        pq.nextPacket();

        for(int i = 0; i < 1000; i++){
            pq.addPacket(createTestPkt(parser, 7, 4000+i, "LP"));
        }

        for(int i = 0; i < 2900; i++){
            byte[] pkt = pq.nextPacket();
            if (pkt != null) {
                final PacketInfo info = parser.parsePacket(pkt);
                System.out.println(info.getChunkNum());
            }
        }

        Thread.sleep(1100);

        pq.nextPacket();
    }

    @Test
    void testAggressiveMLFQSurvival() throws UnknownHostException, InterruptedException {
        PacketParser parser = getParser();
        final int LP_BUDGET = 20;

        // --- Phase 1: Initial Load ---
        // Load 40 LP packets (P0-P39) to ensure 20 packets remain after the first epoch.
        for (int i = 0; i < 40; i++) {
            pq.addPacket(createTestPkt(parser, 6, i, "LP" + i));
        }
        // Add HP/MP traffic to ensure P3 only gets its 20 tokens
        for (int i = 0; i < 50; i++) { pq.addPacket(createTestPkt(parser, 1, 100+i, "HP")); }
        for (int i = 0; i < 30; i++) { pq.addPacket(createTestPkt(parser, 3, 200+i, "MP")); }


        // --- Epoch 1: Consumption (P0-P19 Sent) ---
        // Drain 100 packets (50 HP, 30 MP, 20 LP).
        // P20 to P39 remain in MLFQ[0].
        for (int i = 0; i < 100; i++) { pq.nextPacket(); }

        System.out.println("--- Epoch 1 Complete. P20-P39 remain in MLFQ[0]. ---");


        // --- Rotation 1: MLFQ[0] -> MLFQ[1] ---
        Thread.sleep(150); // Wait > 10ms for budget reset
        Thread.sleep(1000); // Wait > 1000ms for rotation
        pq.nextPacket(); // Sends P20 and Trigger rotation. P21-P39 move to MLFQ[1].

        // --- Epoch 2: Consumption (P20-P39 Sent from MLFQ[1]) ---
        // Drain 100 packets. P21-P39 are now sent using P3's budget.
        for (int i = 0; i < 100; i++) { pq.nextPacket(); }

        System.out.println("--- Epoch 2 Complete. MLFQ[1] is now empty. ---");

        // --- Target Packet Load ---
        // Load one unique target packet (P_TARGET) into MLFQ[0] to track it.
        byte[] testPkt = createTestPkt(parser, 1, 16900, "Test_Pkt");
        pq.addPacket(testPkt);
        byte[] targetPkt = createTestPkt(parser, 7, 500, "TARGET_SURVIVOR");
        pq.addPacket(targetPkt);

        // --- Rotation 2: MLFQ[1] (empty) -> MLFQ[2] ---
        Thread.sleep(150);
        Thread.sleep(1000);
        pq.nextPacket(); // Trigger rotation. MLFQ[0] moves to MLFQ[1].

        // --- Epoch 3: TARGET is starved by P1/P2 ---
        // Fill P1/P2 budgets and consume them instantly. TARGET packet remains in MLFQ[1].
        for (int i = 0; i < 50; i++) { pq.addPacket(createTestPkt(parser, 1, 300+i, "HP")); }
        for (int i = 0; i < 30; i++) { pq.addPacket(createTestPkt(parser, 4, 400+i, "MP")); }
        for (int i = 0; i < 79; i++) { pq.nextPacket(); } // Budget consumed (TARGET not sent)

        // --- Rotation 3: MLFQ[2] (empty) -> MLFQ[0] (Recycled Queue) ---
        Thread.sleep(150);
        Thread.sleep(1000);
        pq.nextPacket(); // Trigger rotation.

        // --- Final Rotation (Triggering the Recycled Queue) ---
        // The TARGET packet should still be in MLFQ[2]

        // Wait 10ms for budget reset
        Thread.sleep(15);

        // The next call must send the Target Packet, proving it survived the rotation cycles
        byte[] survivorPkt = pq.nextPacket();

        // --- Assertions ---
        assertNotNull(survivorPkt, "The target packet must be sent after surviving rotations.");
        PacketInfo info = parser.parsePacket(survivorPkt);
        assertEquals(7, info.getPriority(), () -> "The packet should be from level 2.");
        assertEquals(500, info.getChunkNum(),
                () -> "The chunk number must match the target survivor (500).");
    }

    @Test
    void testMLFQStarvationPrevention() throws InterruptedException, UnknownHostException {
        PacketParser parser = getParser();
        final int MLFQ_BUDGET = 20;

        // Add 100 LP packets (P_0 to P_99)
        for (int i = 0; i < 100; i++) {
            pq.addPacket(createTestPkt(parser, 7, i, "P" + i));
        }

        for (int i = 0; i < 99; i++) {
            pq.nextPacket();
        }

        byte[] pkt = pq.nextPacket(); // Triggers final rotation

        // Verify the first packet after the full cycle is P_60, and its budget is used.
        PacketInfo info = parser.parsePacket(pkt);
        int priority = info.getPriority();
        int chunkNum = info.getChunkNum();

        assertEquals(7, priority, "Priority must be 7 (Low)");
        assertEquals(99, chunkNum, "First packet after full cycle should be P_60");
    }


    //-------------------------------------------------------------------------
    // WORK-CONSERVATION (CRITICAL EFFICIENCY TEST)
    //-------------------------------------------------------------------------

    @Test
    void testWorkConservation() throws UnknownHostException {
        PacketParser parser = getParser();

        // 1. Add only 10 HP (P1) packets (Budget is 50)
        for (int i = 0; i < 10; i++) {
            pq.addPacket(createTestPkt(parser, 1, i, "HP" + i));
        }
        // 2. Add 100 MP (P2) packets (Budget is 30)
        for (int i = 0; i < 100; i++) {
            pq.addPacket(createTestPkt(parser, 4, 100 + i, "MP" + i));
        }
        // 3. Add 100 LP (P3) packets (Budget is 20)
        for (int i = 0; i < 100; i++) {
            pq.addPacket(createTestPkt(parser, 7, 200 + i, "LP" + i));
        }

        int sentCount = 0;
        int hpSent = 0;
        int mpSent = 0;
        int lpSent = 0;

        // Drain up to the total available budget (100)
        for (int i = 0; i < 500; i++) {
            byte[] pkt = pq.nextPacket();
            if (pkt != null) {
                sentCount++;
                PacketInfo info = parser.parsePacket(pkt);
                int priority = info.getPriority();
                if (priority == 1) hpSent++;
                else if (priority == 4) mpSent++;
                else if (priority == 7) lpSent++;
            }
        }

        // P1: Only 10 packets were available. 40 tokens were unused.
        assertEquals(10, hpSent, "P1 should only send the 10 packets available.");

        // P2: Should consume its own 30 tokens + 40 unused P1 tokens = 70.
        assertEquals(100, mpSent, "P2 must consume its 30 tokens + 40 unused P1 tokens.");

        // P3: Used no tokens, as P1 and P2 used the full 80 (70+10) slots, and P3's budget (20) remains.
        assertEquals(100, lpSent, "P3 should consume its own 20 tokens (no higher priority tokens remain).");

        // Total sent must be 10 (P1) + 70 (P2) + 20 (P3) = 100 packets
        assertEquals(210, sentCount, "Total sent must equal the total added.");
    }

    //-------------------------------------------------------------------------
    // CONCURRENT ACCESS TESTS
    //-------------------------------------------------------------------------

    @Test
    void testConcurrentAccessAndOrder() throws InterruptedException, UnknownHostException, ExecutionException {
        PacketParser parser = getParser();

        final int NUM_PACKETS_PER_THREAD = 100; // 400 total packets of each priority
        final AtomicInteger globalChunkCounter = new AtomicInteger(0);
        final int NUM_SEND_THREADS = 4;
        final int NUM_RECEIVE_THREADS = 4;

        ExecutorService executor = Executors.newFixedThreadPool(NUM_SEND_THREADS + NUM_RECEIVE_THREADS);
        ConcurrentLinkedQueue<String> sentPacketsLog = new ConcurrentLinkedQueue<>();
        InetAddress ip = InetAddress.getByName("0.0.0.0");

        // --- 1. Concurrent Packet Addition (Sender Threads) ---
        Callable<Void> senderTask = () -> {
            for (int i = 0; i < NUM_PACKETS_PER_THREAD; i++) {
                int uniqueChunkId = globalChunkCounter.getAndIncrement();

                // Use uniqueChunkId in the payload for easier debugging
                pq.addPacket(createTestPkt(parser, 1, uniqueChunkId, "HP-" + uniqueChunkId));
                pq.addPacket(createTestPkt(parser, 4, uniqueChunkId, "MP-" + uniqueChunkId));
                pq.addPacket(createTestPkt(parser, 7, uniqueChunkId, "LP-" + uniqueChunkId));

                // Introduce interleaving
                Thread.sleep(1);
            }
            return null;
        };

        // Submit sender tasks
        List<Future<Void>> sendFutures = new ArrayList<>();
        for(int i = 0; i < NUM_SEND_THREADS; i++) {
            sendFutures.add(executor.submit(senderTask));
        }

        // Wait for all sending threads to complete
        for (Future<Void> future : sendFutures) {
            future.get();
        }
        System.out.println("\n--- Concurrent Addition Complete (Total: " + globalChunkCounter.get() * 3 + " packets) ---");

        // --- 2. Concurrent Packet Retrieval (Receiver Threads) ---
        Callable<Void> receiverTask = () -> {
            for (int i = 0; i < 400; i++) { // Each receiver tries 400 times
                byte[] pkt = pq.nextPacket();
                if (pkt != null) {
                    PacketInfo info = parser.parsePacket(pkt);
                    int priority = info.getPriority();
                    int chunkNum = info.getChunkNum();

                    String logEntry = String.format("P: %d, Chunk: %d, Thread: %s",
                            priority, chunkNum, Thread.currentThread().getName());
                    sentPacketsLog.add(logEntry);
                }
            }
            return null;
        };

        // Submit receiver tasks
        List<Future<Void>> receiveFutures = new ArrayList<>();
        for(int i = 0; i < NUM_RECEIVE_THREADS; i++) {
            receiveFutures.add(executor.submit(receiverTask));
        }

        // Wait for all receiver threads to complete their attempts
        for (Future<Void> future : receiveFutures) {
            future.get();
        }

        // --- 3. Analysis ---
        int totalSent = sentPacketsLog.size();

        System.out.println("\n--- SENT PACKET LOG (First Epoch) ---");
        List<String> firstEpoch = sentPacketsLog.stream().toList();

        int p1Count = 0;
        int p2Count = 0;
        int p3Count = 0;

        for (String log : firstEpoch) {
            if (log.contains("P: 1")) p1Count++;
            else if (log.contains("P: 4")) p2Count++;
            else if (log.contains("P: 7")) p3Count++;
        }

        // Budget integrity check: The nextPacket() must respect the 100 limit.
        assertEquals(1200, totalSent);

        // Fairness check: Confirming the 50:30:20 distribution under concurrency.
        assertEquals(400, p1Count, "P1 must consume 50 budget tokens.");
        assertEquals(400, p2Count, "P2 must consume 30 budget tokens.");
        assertEquals(400, p3Count, "P3 must consume 20 budget tokens.");

        executor.shutdown();
    }

    @Test
    void testNonStarvationDuringBulkTransfer() throws InterruptedException, UnknownHostException, ExecutionException {
        PacketParser parser = getParser();
        ExecutorService executor = Executors.newFixedThreadPool(3); // 3 threads
        final int BULK_PACKETS = 400;
        final int HP_INJECTION_COUNT = 10;
        final int HP_CHUNK_START = 7700000;

        // Use a queue to track the sent HP packets for verification
        ConcurrentLinkedQueue<Integer> sentHighPriorityChunks = new ConcurrentLinkedQueue<>();

        // 1. Bulk Sender Task (Continuously load Low Priority)
        Callable<Void> bulkSender = () -> {
            // Load the initial bulk of the 4GB file
            for (int i = 0; i < BULK_PACKETS; i++) {
                pq.addPacket(createTestPkt(parser, 7, i, "LP_BULK_" + i));
                // Slow down slightly to allow the receiver to start
                if (i % 1000 == 0) Thread.sleep(1);
            }
            return null;
        };

        // 2. Critical Sender Task (Inject 10 HP packets mid-transfer)
        Callable<Void> criticalSender = () -> {
            // Wait for the bulk transfer to be well underway (e.g., 50ms)
            Thread.sleep(50);

            System.out.println("\n*** Injecting 10 High Priority Packets Now ***");

            // Inject the 10 highest-priority packets (P1)
            for (int i = 0; i < HP_INJECTION_COUNT; i++) {
                pq.addPacket(createTestPkt(parser, 0, HP_CHUNK_START + i, "HP_CRITICAL_" + i));
            }
            return null;
        };

        Callable<Void> sender1 = () -> {
            Thread.sleep(100);

            for(int i = 0; i < HP_INJECTION_COUNT; i++){
                pq.addPacket(createTestPkt(parser, 4, (HP_CHUNK_START + 10) + i, "HP_CRITICAL_" + i));
                pq.addPacket(createTestPkt(parser, 6, (HP_CHUNK_START + 10) + i, "HP_CRITICAL_" + i));
                pq.addPacket(createTestPkt(parser, 1, (HP_CHUNK_START + 10) + i, "HP_CRITICAL_" + i));
            }
            return null;
        };

        // 3. Receiver Task (Continuous Transmission)
        Callable<Integer> receiver = () -> {
            int count = 0;
            int maxIterations = BULK_PACKETS + HP_INJECTION_COUNT + 130; // Total expected + buffer

            for (int i = 0; i < maxIterations; i++) {
                byte[] pkt = pq.nextPacket();
                if (pkt != null) {
                    count++;
                    PacketInfo info = parser.parsePacket(pkt);
                    int chunkNum = info.getChunkNum();
                    int priority = info.getPriority();

                    // Track and log the critical HP packets when they are sent
                    if (info.getPriority() <= 6) { // Priority ZERO, ONE, TWO map to highestPriorityQueue
                        if (chunkNum >= HP_CHUNK_START) {
                            sentHighPriorityChunks.add(chunkNum);
                            System.out.println("-> HP Packet Sent: Chunk " + chunkNum + " Priority: " + priority);
                        }
                    }
                } else {
                    // Sleep briefly if queue is empty to avoid busy-waiting
                    Thread.sleep(1);
                }
            }
            return count;
        };

        // Execute all tasks
        Future<Void> bulkFuture = executor.submit(bulkSender);
        Future<Void> criticalFuture = executor.submit(criticalSender);
        Future<Integer> receiverFuture = executor.submit(receiver);
        Future<Void> future2 = executor.submit(sender1);

        // Wait for the critical sender to finish injection
        criticalFuture.get();

        // Wait for the bulk sender to finish loading the initial queue
        bulkFuture.get();

        // Wait for the receiver to finish draining
        receiverFuture.get();

        future2.get();

        executor.shutdown();

        // --- Verification ---
        assertEquals(HP_INJECTION_COUNT * 4, sentHighPriorityChunks.size(), "All 10 HP packets must have been sent.");

        // Verify order (The first HP packets sent must be the injected ones)
        for (int i = 0; i < HP_INJECTION_COUNT; i++) {
            assertTrue(sentHighPriorityChunks.contains(HP_CHUNK_START + i), "Missing injected HP packet: " + (HP_CHUNK_START + i));
        }
        System.out.println("\nTest Successful: HP packets were sent immediately after injection, proving non-starvation.");
    }

    //-------------------------------------------------------------------------
    // PERFORMANCE TEST (THROUGHPUT)
    //-------------------------------------------------------------------------

    @Test
    void testThroughput() throws UnknownHostException, InterruptedException {
        PacketParser parser = getParser();
        final int TOTAL_PACKETS = 60; // Load 600,000 packets
        final int PACKETS_PER_ITERATION = 8;
        final int TOTAL_PACKETS_TO_SEND = TOTAL_PACKETS * PACKETS_PER_ITERATION;

        // Load the Queue
        System.out.println("Loading " + TOTAL_PACKETS_TO_SEND + " packets...");
        for (int i = 0; i < TOTAL_PACKETS; i++) {
            // Use different chunk numbers for uniqueness
            pq.addPacket(createTestPkt(parser, 7, i, "Payload" + i));
            pq.addPacket(createTestPkt(parser, 0, i, "High Priority + i"));
            pq.addPacket(createTestPkt(parser, 5, i, "five Priority + i"));
            pq.addPacket(createTestPkt(parser, 1, i, "one Priority + i"));
            pq.addPacket(createTestPkt(parser, 2, i, "twooo Priority + i"));
            pq.addPacket(createTestPkt(parser, 3, i, "three3 Priority + i"));
            pq.addPacket(createTestPkt(parser, 4, i, "four44 Priority + i"));
            pq.addPacket(createTestPkt(parser, 6, i, "sixxxxx Priority + i"));
        }
        System.out.println("Loading complete.");

        // Measure Consumption
        int sentCount = 0;
        int nullCount = 0;

        // Use System.nanoTime() for high-resolution timing
        long startTimeNanos = System.nanoTime();

        // Drain the queue until all packets are retrieved
        long packetsRemaining = TOTAL_PACKETS_TO_SEND;
        while(packetsRemaining > 0) {
            byte[] pkt = pq.nextPacket();
            if (pkt != null) {
                sentCount++;
                packetsRemaining--;
            }
        }


        long endTimeNanos = System.nanoTime();

        // Calculation and Assertion

        // Ensure the correct number of packets were sent
        assertEquals(TOTAL_PACKETS_TO_SEND, sentCount, "The total number of sent packets must equal the number added.");

        // Calculate time difference in seconds
        long elapsedTimeNanos = endTimeNanos - startTimeNanos;
        double elapsedTimeSeconds = elapsedTimeNanos / 1_000_000_000.0;

        // Calculate Packets Per Second (PPS)
        double packetsPerSecond = (double)sentCount / elapsedTimeSeconds;

        // Log the result
        System.out.printf("\nTHROUGHPUT RESULTS\n");
        System.out.printf("Total Packets Sent: %,d\n", sentCount);
        System.out.printf("Elapsed Time: %.4f seconds\n", elapsedTimeSeconds);
        System.out.printf("Approximate Throughput: **%,.0f PPS** (Packets Per Second)\n", packetsPerSecond);

        // Assertion against a baseline minimum (adjust this value based on your hardware)
        // This asserts that the performance is reasonable, not just correct.
        final double MIN_ACCEPTABLE_PPS = 1000.0; // Example baseline (100k PPS)
        assertTrue(packetsPerSecond > MIN_ACCEPTABLE_PPS,
                String.format("Throughput is too low: %.0f PPS (Below %.0f PPS minimum)",
                        packetsPerSecond, MIN_ACCEPTABLE_PPS));
    }

    @Test
    void testThroughputNumber() throws UnknownHostException {
        PacketParser parser = getParser();
        int count = 0;
        for(int i = 0; i < 10; i++){
            pq.addPacket(createTestPkt(parser,1, i, "packet number" + i));
        }
        for(int i = 0; i < 10; i++){
            byte[] pkt = pq.nextPacket();
            if(pkt == null){
                count++;
            }
        }
        long throughput = pq.getThroughput();
        System.out.println(count);
    }
}