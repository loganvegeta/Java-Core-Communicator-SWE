package com.swe.networking;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for splitting the packets.
 */
public class SplitPacketTest {
    @org.junit.jupiter.api.Test
    void testSingleCompletePacket() {
        final SplitPackets splitter = SplitPackets.getSplitPackets();
        final byte[] packet = new byte[] { 0x06, 0x00, 1, 2, 3, 4 };
        final List<byte[]> result = splitter.split(packet);
        assertEquals(1, result.size());
        System.out.println(Arrays.toString(result.get(0)));
        assertArrayEquals(new byte[] { 0x06, 0x00, 1, 2, 3, 4 }, result.get(0));
    }

    @org.junit.jupiter.api.Test
    void testMultipleCompletePackets() {
        final SplitPackets splitter = SplitPackets.getSplitPackets();
        final byte[] data = new byte[] {
                0x06, 0x00, 1, 2, 3, 4,
                0x05, 0x00, 5, 6, 7
        };
        final List<byte[]> result = splitter.split(data);
        System.out.println(Arrays.toString(result.get(0)));
        System.out.println(Arrays.toString(result.get(1)));
        assertEquals(2, result.size());
        assertArrayEquals(new byte[] { 0x06, 0x00, 1, 2, 3, 4 }, result.get(0));
        assertArrayEquals(new byte[] { 0x05, 0x00, 5, 6, 7 }, result.get(1));
    }

    @org.junit.jupiter.api.Test
    void testFragmentedPacketAcrossCalls() {
        final SplitPackets splitter = SplitPackets.getSplitPackets();

        final byte[] part1 = new byte[] { 0x08, 0x00, 9, 8 };
        final List<byte[]> result1 = splitter.split(part1);
        assertTrue(result1.isEmpty());
        final byte[] part2 = new byte[] { 7, 6, 5, 4 };
        final List<byte[]> result2 = splitter.split(part2);
        assertEquals(1, result2.size());
        assertArrayEquals(new byte[] { 0x08, 0x00, 9, 8, 7, 6, 5, 4 }, result2.get(0));
    }

    @org.junit.jupiter.api.Test
    void testMultiplePacketsWithLastFragmented() {
        final SplitPackets splitter = SplitPackets.getSplitPackets();

        final byte[] data = new byte[] { 0x05, 0x00, 11, 12, 13, 0x07, 0x00, 14 };
        final List<byte[]> result = splitter.split(data);
        assertEquals(1, result.size());
        assertArrayEquals(new byte[] { 0x05, 0x00, 11, 12, 13 }, result.get(0));
        final byte[] remainder = new byte[] { 15, 16, 17, 18 };
        final List<byte[]> result2 = splitter.split(remainder);
        assertEquals(1, result2.size());
        assertArrayEquals(new byte[] { 0x07, 0x00, 14, 15, 16, 17, 18 }, result2.get(0));
    }

    @org.junit.jupiter.api.Test
    void testEmptyData() {
        final SplitPackets splitter = SplitPackets.getSplitPackets();
        final byte[] empty = new byte[0];
        final List<byte[]> result = splitter.split(empty);
        assertTrue(result.isEmpty());
    }
}
