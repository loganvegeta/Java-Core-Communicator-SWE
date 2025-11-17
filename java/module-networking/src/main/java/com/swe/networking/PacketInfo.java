package com.swe.networking;

import java.net.InetAddress;
import java.util.Arrays;

/**
 * Simple data-holder for packet attributes. The structure only stores fields.
 */
public class PacketInfo {

    /** Payload bytes (may be empty, copied on access). */
    private byte[] payload = new byte[0];

    /** Length of the packet (2 bytes). */
    private int length;

    /** Packet type (2 bits ). */
    private int type;

    /** Packet priority (3 bits ). */
    private int priority;

    /** Module identifier (4 bits ). */
    private int module;

    /** Connection type (3 bits ). */
    private int connectionType;

    /** Broadcast flag (1 bit ). */
    private int broadcast;

    /** IPv4 address (4 bytes). */
    private InetAddress ipAddress;

    /** Network port number (16-bit value). */
    private int portNum;

    /** Message identifier (32-bit). */
    private int messageId;

    /** Chunk number for multi-part messages (32-bit). */
    private int chunkNum;

    /** Length of this chunk in bytes (32-bit). */
    private int chunkLength;

    public PacketInfo() {
    }

    /**
     * Get a copy of the payload bytes.
     *
     * @return copy of the payload
     */
    public byte[] getPayload() {
        return Arrays.copyOf(payload, payload.length);
    }

    /**
     * Set the payload bytes. A copy is made; null becomes an empty array.
     *
     * @param payloadBytes payload bytes or null
     */
    public void setPayload(final byte[] payloadBytes) {
        if (payloadBytes == null) {
            this.payload = new byte[0];
        } else {
            this.payload = Arrays.copyOf(payloadBytes, payloadBytes.length);
        }
    }

    /** Get the length of the packet.
     * @return length the length of the packet
     */
    public int getLength() {
        return length;
    }

    /** Set the length of the packet.
     * @param lengthValue the length of the packet
     */
    public void setLength(final int lengthValue) {
        this.length = lengthValue;
    }

    /** Get the packet type.
     * @return type the packet type
     */
    public int getType() {
        return type;
    }
   
    /** Set the packet type.
     * @param typeValue the packet type
     */
    public void setType(final int typeValue) {
        this.type = typeValue;
    }

    /** Get the packet priority.
     * @return priority the packet priority
     */
    public int getPriority() {
        return priority;
    }
   
    /** Set the packet priority.
     * @param priorityValue the packet priority
     */
    public void setPriority(final int priorityValue) {
        this.priority = priorityValue;
    }

    /** Get the module id.
     * @return module the module id
     */
    public int getModule() {
        return module;
    }
   
    /** Set the module id.
     * @param moduleValue the module id
     */
    public void setModule(final int moduleValue) {
        this.module = moduleValue;
    }

    /** Get the connection type.
     * @return connectionType the connection type
     */
    public int getConnectionType() {
        return connectionType;
    }
   
    /** Set the connection type.
     * @param connectionTypeValue the connection type
     */
    public void setConnectionType(final int connectionTypeValue) {
        this.connectionType = connectionTypeValue;
    }

    /** Get the broadcast flag (0 or 1).
     * @return broadcast the broadcast flag
     */
    public int getBroadcast() {
        return broadcast;
    }
   
    /** Set the broadcast flag (0 or 1).
     * @param broadcastValue the broadcast flag
     */
    public void setBroadcast(final int broadcastValue) {
        this.broadcast = broadcastValue;
    }

    /** Get the IPv4 address.
     * @return ipAddress the IPv4 address
     */
    public InetAddress getIpAddress() {
        return ipAddress;
    }
    
    /** Set the IPv4 address.
     * @param ipAddressValue the IPv4 address
     */
    public void setIpAddress(final InetAddress ipAddressValue) {
        this.ipAddress = ipAddressValue;
    }

    /** Get the port number.
     * @return portNum the port number
     */
    public int getPortNum() {
        return portNum;
    }
    
    /** Set the port number.
     * @param portNumValue the port number
     */
    public void setPortNum(final int portNumValue) {
        this.portNum = portNumValue;
    }

    /** Get the message id.
     * @return messageId the message id
    */
    public int getMessageId() {
        return messageId;
    }
    
    /** Set the message id.
     * @param messageIdValue the message id
     */
    public void setMessageId(final int messageIdValue) {
        this.messageId = messageIdValue;
    }

    /** Get the chunk number.
     * @return chunkNum the chunk number
     */
    public int getChunkNum() {
        return chunkNum;
    }
    
    /** Set the chunk number.
     * @param chunkNumValue the chunk number
     */
    public void setChunkNum(final int chunkNumValue) {
        this.chunkNum = chunkNumValue;
    }

    /** Get the chunk length in bytes.
     * @return chunkLength the chunk length in bytes
     */
    public int getChunkLength() {
        return chunkLength;
    }
    
    /** Set the chunk length in bytes. 
     * @param chunkLengthValue the chunk length in bytes.
    */
    public void setChunkLength(final int chunkLengthValue) {
        this.chunkLength = chunkLengthValue;
    }

}