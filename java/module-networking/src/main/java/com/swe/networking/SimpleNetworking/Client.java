package com.swe.networking.SimpleNetworking;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.swe.networking.ClientNode;
import com.swe.networking.ModuleType;
import com.swe.networking.PacketInfo;
import com.swe.networking.PacketParser;
import com.swe.networking.ProtocolBase;
import com.swe.networking.SplitPackets;
import com.swe.networking.TCPCommunicator;

// File owned by Loganath
/**
 * The main module of the client device.
 */
public class Client implements IUser {

    /**
     * Variable to store the name of the module.
     */
    private static final String MODULENAME = "[CLIENT]";
    /**
     * The variable to store the device IP address.
     */
    private final String deviceIp;
    /**
     * The variable to store the device port number.
     */
    private final int devicePort;
    /**
     * The variable used by server to accept connections from clients.
     */
    private final ProtocolBase communicator;
    /**
     * The singleton class object for packet parser.
     */
    private final PacketParser parser;
    /**
     * The singleton class object for simplenetworking.
     */
    private final SimpleNetworking simpleNetworking;
    /**
     * The variable to store the module type.
     */
    private final ModuleType moduleType = ModuleType.NETWORKING;
    /**
     * The variable to store chunk Manager.
     */
    private final SimpleChunkManager chunkManager;

    /**
     * The variable to store chunk Manager payload size.
     */
    private final int payloadSize = 15 * 1024;

    /**
     * The constructor function for the client class.
     *
     * @param deviceAddr the device IP address details
     */
    public Client(final ClientNode deviceAddr) {
        deviceIp = deviceAddr.hostName();
        devicePort = deviceAddr.port();
        parser = PacketParser.getPacketParser();
        simpleNetworking = SimpleNetworking.getSimpleNetwork();
        communicator = new TCPCommunicator(devicePort);
        chunkManager = SimpleChunkManager.getChunkManager(payloadSize);
        SimpleNetworkLogger.printInfo(MODULENAME, "Client initialized...");
    }

    /**
     * Function to send the data to a list of destination.
     *
     * @param data the data to be sent
     * @param destIp the list fo destination to send the data
     * @param serverIp the Ip address of the main server
     * @param module the module to send th data to
     */
    @Override
    public void send(final byte[] data, final ClientNode[] destIp, final ClientNode serverIp, final ModuleType module) {
        for (ClientNode client : destIp) {
            SimpleNetworkLogger.printInfo(MODULENAME, "Data size sent : " + data.length);
            communicator.sendData(data, serverIp);
            SimpleNetworkLogger.printInfo(MODULENAME, "Data sent successfully...");
        }
    }

    /**
     * Function to receive data from the given socket.
     */
    @Override
    public void receive() throws IOException {
        final byte[] packet = communicator.receiveData();
        if (packet != null) {
            final List<byte[]> packets = SplitPackets.getSplitPackets().split(packet);
            for (byte[] p : packets) {
                parsePacket(p);
            }
        }
    }

    /**
     * Function to parse the received packet and perform required response.
     *
     * @param packet the packet to parse
     */
    public void parsePacket(final byte[] packet) {
        try {
            final PacketInfo pktInfo = parser.parsePacket(packet);
            final int module = pktInfo.getModule();
            final ModuleType type = moduleType.getType(module);
            final String data = new String(pktInfo.getPayload(), StandardCharsets.UTF_8);
            SimpleNetworkLogger.printInfo(MODULENAME, "Client data size received : " + data.length());
            SimpleNetworkLogger.printInfo(MODULENAME, "Client module received : " + type.toString());
            byte[] message = chunkManager.addChunk(packet);
            if (message != null) {
                final PacketInfo newpktInfo = parser.parsePacket(message);
                message = newpktInfo.getPayload();
                simpleNetworking.callSubscriber(message, type);
            }
        } catch (UnknownHostException ex) {
            SimpleNetworkLogger.printError(MODULENAME, "Client Could not parse packet succesfully...");
        }
    }

    /**
     * Function to be called on closing.
     */
    @Override
    public void closeUser() {
        SimpleNetworkLogger.printInfo(MODULENAME, "Closing the receving socket...");
        communicator.close();
    }
}
