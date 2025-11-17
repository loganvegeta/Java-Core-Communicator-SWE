package com.swe.networking;

import java.nio.ByteBuffer;

/**
 * The class implementing coalescing receive and
 * passing data to corresponding subscribed module.
 */
public class CoalesceReceive {

    /**
     * The module name for logging.
     */
    private static final String MODULENAME = "[COALESCERECEIVE]";

    /**
     * function to parse coalesce packet and pass to corresponding listener.
     *
     * @param coalescedData  coalesced payload.
     */
    public void receiveCoalescedPacket(final ByteBuffer coalescedData) {
        NetworkLogger.printInfo(MODULENAME, "Receiving coalesced packet of size: " + coalescedData.remaining());
        while (coalescedData.hasRemaining()) {
            // Get the size of the packet
            final int packetSize = coalescedData.getInt();
            NetworkLogger.printInfo(MODULENAME, "Extracted packet size: " + packetSize);

            // Get the module type
            final byte moduleTypeByte = coalescedData.get();
            final int moduleTypeInt = moduleTypeByte;
            NetworkLogger.printInfo(MODULENAME, "Extracted module type: " + moduleTypeInt);

            // Get the payload
            final int payloadSize = packetSize - 4 - 1;
            final byte[] payload = new byte[payloadSize];
            coalescedData.get(payload);

            // Call the module message listener based on the module type
            NetworkLogger.printInfo(MODULENAME, "Dispatching packet to module: " + moduleTypeInt);
            Networking.getNetwork().callSubscriber(moduleTypeInt, payload);
        }
    }
}