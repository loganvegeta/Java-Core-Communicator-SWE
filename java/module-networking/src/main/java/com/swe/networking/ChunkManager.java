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
     * Singleton chunkManger.
     */
    private static ChunkManager chunkManager = null;
    /**
     * Payload Size.
     */
    private final int defaultPayloadSize; // size of payload in bytes
    /**
     * packetParser.
     */
    private final PacketParser parser = PacketParser.getPacketParser();
    /**
     * headerSize.
     */
    private final int headerSize = PacketParser.getHeaderSize();

    private ChunkManager(final int payloadSize) {
        defaultPayloadSize = payloadSize;
    }

    /**
     * Singleton chunkManager getter.
     *
     * @param payloadSize the payloadsize for the chunkManager
     * @return chunkManager.
     */
    public static ChunkManager getChunkManager(final int payloadSize) {
        if (chunkManager == null) {
            chunkManager = new ChunkManager(payloadSize);
            return chunkManager;
        }
        return chunkManager;
    }

    /**
     * Message id.
     */
    private int messageId = 0;

    /**
     * chunkListMap maps message id to list of chunks.
     */
    private final Map<String, Vector<byte[]>> chunkListMap = new HashMap<>();
    /**
     * messageList gets the list of merged chunks.
     */
    private final Vector<byte[]> messageList = new Vector<>();

    /**
     * Temporary function used to get the messageList. Used only for testing and
     * to be replaced with some other function later.
     *
     * @return messageList. All the messages recorded till now.
     */
    Vector<byte[]> getMessageList() {
        return messageList;
    }

    /**
     * Add chunk function.
     *
     * @param chunk the byte of chunk coming in.
     * @return the combined chunk if present
     * @throws UnknownHostException the issue from packet parser.
     */
    public byte[] addChunk(final byte[] chunk) throws UnknownHostException {
        final PacketInfo info = parser.parsePacket(chunk);
        final String msgId = String.valueOf(info.getMessageId()) + ":" + info.getIpAddress().toString();
        final int maxNumChunks = info.getChunkLength();
        final int chunkId = info.getChunkNum();
        System.out.println("Chunk id / total chunks " + chunkId + " / " + maxNumChunks);
        if (chunkListMap.containsKey(msgId)) {
            chunkListMap.get(msgId).add(chunk);
        } else {
            chunkListMap.put(msgId, new Vector<byte[]>());
            chunkListMap.get(msgId).add(chunk);
        }
        if (chunkListMap.get(msgId).size() == maxNumChunks) {
            final byte[] messageChunk = mergeChunks(chunkListMap.get(msgId));
            // TOD use appropriate function once the message is ready
            messageList.add(messageChunk);
            chunkListMap.remove(msgId);
            return messageChunk;
        }
        return null;
    }

    /**
     * Merge Chunks function.
     *
     * @param chunks The list of incoming chunks.
     * @return merged packet.
     */
    private byte[] mergeChunks(final Vector<byte[]> chunks) throws UnknownHostException {
        int dataSize = 0;
        for (byte[] chunk : chunks) {
            dataSize += chunk.length - headerSize;
        }
        final byte[] completePayload = new byte[dataSize];
        PacketInfo info = parser.parsePacket(chunks.get(0));
        final int lastChunkNum = info.getChunkLength() - 1;
        final Vector<byte[]> sortedChunks = new Vector<>(Collections.nCopies(chunks.size(), null));
        for (byte[] chunk : chunks) {
            info = parser.parsePacket(chunk);
            final int i = info.getChunkNum();
            sortedChunks.set(i, chunk);
        }
        int i = 0;
        for (byte[] chunk : sortedChunks) {
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
     *
     * @param info The Chunk information including payload of the message
     * @param payloadSize The payload size of the message.
     * @return chunks The message broken into list of chunks.
     */
    public Vector<byte[]> chunk(final PacketInfo info, final int payloadSize) {
        // List of chunked packets we will be returning
        final Vector<byte[]> chunks = new Vector<>();

        final byte[] data = info.getPayload();
        final int numChunks = (data.length + payloadSize - 1) / payloadSize;
//        System.out.println("chunk length " + numChunks);
        info.setChunkLength(numChunks);
        info.setMessageId(messageId);
        messageId++;
        // reset message id to zero once it exceed limit
        for (int i = 0; i < data.length; i += payloadSize) {
            final int pSize = Math.min(payloadSize, data.length - i);
//            System.out.println("payload size " + pSize);
            final byte[] payloadChunk = new byte[pSize];
            System.arraycopy(data, i, payloadChunk, 0, pSize);
            final int chunkNumber = i / payloadSize;
            info.setChunkNum(chunkNumber);
            info.setPayload(payloadChunk);
            info.setLength(PacketParser.getHeaderSize() + pSize);
            final byte[] pkt = parser.createPkt(info);
            chunks.add(pkt);
        }
//        System.out.println("Chunk size : " + chunks.size());
        return chunks;
    }

    public Vector<byte[]> chunk(final PacketInfo info) {
        return chunk(info, defaultPayloadSize);
    }

    /**
     * get last message id.
     *
     * @return messageId.
     */
    public int getLastMessageId() {
        return messageId - 1;
    }
}
