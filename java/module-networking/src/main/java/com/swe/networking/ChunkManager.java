package com.swe.networking;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Code for Chunk Manager.
 */
public class ChunkManager {
    /**
     * Payload Size.
     */

    private final int payloadSize; // size of payload in bytes
    /**
     * packetParser.
     */
    private final PacketParser parser = PacketParser.getPacketParser();
    /**
     * headerSize.
     */
    private final int headerSize = PacketParser.getHeaderSize();

    ChunkManager(final int chunkPayloadSize) {
        this.payloadSize = chunkPayloadSize;
    }

    /**
     * Chunking function.
     * @param chunks The List of chunks.
     * @return messageMap The map of message_id to its corresponding list of chunks.
     */
    Map<Integer, Vector<byte[]>> groupChunks(final Vector<byte[]> chunks) throws UnknownHostException {
        final Map<Integer, Vector<byte[]>> messageMap = new HashMap<>();
        for (byte[] chunk:chunks) {
            final PacketInfo info = parser.parsePacket(chunk);
            final int messageId = info.getMessageId();
            final Vector<byte[]> groupedChunks;
            if (!messageMap.containsKey(messageId)) {
                groupedChunks = new Vector<>();
                groupedChunks.add(chunk);
                messageMap.put(messageId, groupedChunks);
            } else {
                groupedChunks = messageMap.get(messageId);
                groupedChunks.add(chunk);
            }
        }
        return messageMap;
    }


    /**
     * Merge Chunks function.
     * @param chunks The list of incoming chunks.
     * @return merged packet.
     */
    byte[] mergeChunks(final Vector<byte[]> chunks) throws UnknownHostException {
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("there should be at least one chunk in the input array");
        }
        int dataSize = 0;
        for (byte[] chunk:chunks) {
            dataSize += chunk.length - headerSize;
        }
        final byte[] completePayload = new byte[dataSize];
        PacketInfo info = parser.parsePacket(chunks.get(0));
        final int lastChunkNum = info.getChunkLength() - 1;
        if (chunks.size() - 1 != lastChunkNum) {
            throw new IllegalArgumentException("Did not receive all the chunks");
        }
        final Vector<byte[]> sortedChunks = new Vector<>(Collections.nCopies(chunks.size(), null));
        for (byte[] chunk:chunks) {
            info = parser.parsePacket(chunk);
            final int i = info.getChunkNum();
            sortedChunks.set(i, chunk);
        }
        int i = 0;
        for (byte[] chunk:sortedChunks) {
            info = parser.parsePacket(chunk);
            final int chunkSize = chunk.length - headerSize;
            final byte[] chunkPayload = info.getPayload();
            System.arraycopy(chunkPayload, 0, completePayload, i, chunkSize);
            i += chunkSize;
        }
        info = parser.parsePacket(chunks.get(0));
        info.setChunkLength(1);
        info.setChunkNum(0);
        info.setPayload(completePayload);
        return parser.createPkt(info);
    }

    /**
     * Chunking function.
     * @param info The Chunk information including payload of the message
     * @return chunks The message broken into list of chunks.
     */
    public Vector<byte[]> chunk(final PacketInfo info) {
        // List of chunked packets we will be returning
        final Vector<byte[]> chunks = new Vector<>();

        // Type is currently set to 0 : Send Packet to Cluster
        // But we need a function in topology to identify the type
        info.setType(0);
        final byte[] data = info.getPayload();
        final int numChunks = (data.length + payloadSize - 1) / payloadSize;
        info.setChunkLength(numChunks);
        for (int i = 0; i < data.length; i += payloadSize) {
            final int pSize = Math.min(payloadSize, data.length - i);
            final byte[] payloadChunk = new byte[pSize];
            System.arraycopy(data, i, payloadChunk, 0, pSize);
            final int chunkNumber = i / payloadSize;
            info.setChunkNum(chunkNumber);
            info.setPayload(payloadChunk);
            final byte[] pkt = parser.createPkt(info);
            chunks.add(pkt);
        }
        return chunks;
    }
}
