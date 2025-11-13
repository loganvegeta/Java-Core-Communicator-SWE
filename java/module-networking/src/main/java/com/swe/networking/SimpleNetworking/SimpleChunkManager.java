package com.swe.networking.SimpleNetworking;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.swe.networking.PacketInfo;
import com.swe.networking.PacketParser;

/**
 * Code for Chunk Manager.
 */
public class SimpleChunkManager {

    /**
     * Variable to store the name of the module.
     */
    private static final String MODULENAME = "[SIMPLECHUNKMANAGER]";
    /**
     * Singleton chunkManger.
     */
    private static SimpleChunkManager chunkManager = null;
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
    /**
     * Message id.
     */
    private int messageId = 0;

    private SimpleChunkManager(final int payloadSize) {
        SimpleNetworkLogger.printInfo(MODULENAME, "Simple Chunk manager initialized...");
        defaultPayloadSize = payloadSize;
    }

    /**
     * Singleton chunkManager getter.
     *
     * @param payloadSize the payloadsize for the chunkManager
     * @return chunkManager.
     */
    public static SimpleChunkManager getChunkManager(final int payloadSize) {
        if (chunkManager == null) {
            chunkManager = new SimpleChunkManager(payloadSize);
            return chunkManager;
        }
        SimpleNetworkLogger.printInfo(MODULENAME, "Passing already initialized Simple Chunk Manager...");
        return chunkManager;
    }

    /**
     * chunkListMap maps message id to list of chunks.
     */
    private final Map<Integer, Vector<byte[]>> chunkListMap = new HashMap<>();
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
     * @return the combined message if present
     * @throws UnknownHostException the issue from packet parser.
     */
    public byte[] addChunk(final byte[] chunk) throws UnknownHostException {
        final PacketInfo info = parser.parsePacket(chunk);
        final int msgId = info.getMessageId();
        final int maxNumChunks = info.getChunkLength();
        final int chunkNum = info.getChunkNum();
        System.out.println("Message ID: " + msgId);
        System.out.println("Chunk num / Max chunks: " + chunkNum + " / " + maxNumChunks);
        if (chunkListMap.containsKey(msgId)) {
            chunkListMap.get(msgId).add(chunk);
        } else {
            chunkListMap.put(msgId, new Vector<byte[]>());
            chunkListMap.get(msgId).add(chunk);
        }
        if (chunkListMap.get(msgId).size() == maxNumChunks) {
            final byte[] messageChunk = mergeChunks(chunkListMap.get(msgId));
            System.out.println("Merged Message ID: " + msgId + " Size: " + messageChunk.length);
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
        info.setChunkLength(numChunks);
        info.setMessageId(messageId);
        messageId++;
        // reset message id to zero once it exceed limit
        for (int i = 0; i < data.length; i += payloadSize) {
            final int pSize = Math.min(payloadSize, data.length - i);
            final byte[] payloadChunk = new byte[pSize];
            System.arraycopy(data, i, payloadChunk, 0, pSize);
            final int chunkNumber = i / payloadSize;
            info.setChunkNum(chunkNumber);
            info.setPayload(payloadChunk);
            info.setLength(headerSize + pSize);
            final byte[] pkt = parser.createPkt(info);
            chunks.add(pkt);
        }
        return chunks;
    }

    public Vector<byte[]> chunk(final PacketInfo info) {
        return chunk(info, defaultPayloadSize);
    }
}
