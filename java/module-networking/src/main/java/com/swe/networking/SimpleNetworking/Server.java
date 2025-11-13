package com.swe.networking.SimpleNetworking;

import java.io.IOException;
import java.net.InetAddress;
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

//File owned by Loganath.
/**
 * The main class for the server device.
 */
public class Server implements IUser {

    /**
     * Variable to store the name of the module.
     */
    private static final String MODULENAME = "[SERVER]";
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
     * The constructor function for the server class.
     *
     * @param deviceAddr the device IP address details
     */
    public Server(final ClientNode deviceAddr) {
        deviceIp = deviceAddr.hostName();
        devicePort = deviceAddr.port();
        parser = PacketParser.getPacketParser();
        simpleNetworking = SimpleNetworking.getSimpleNetwork();
        communicator = new TCPCommunicator(devicePort);
        chunkManager = SimpleChunkManager.getChunkManager(payloadSize);
        SimpleNetworkLogger.printInfo(MODULENAME, "Server initialized...");
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
    public void send(final byte[] data, final ClientNode[] destIp,
            final ClientNode serverIp, final ModuleType module) {
        for (ClientNode client : destIp) {
            communicator.sendData(data, client);
            SimpleNetworkLogger.printInfo(MODULENAME, "Sending data of size : " + data.length);
            SimpleNetworkLogger.printInfo(MODULENAME, "Destination : " + client);
        }
    }

    /**
     * Function to receive data from the given socket.
     */
    @Override
    public void receive() throws IOException {
        while (true) {
            final byte[] packet = communicator.receiveData();
            if (packet != null) {
                final List<byte[]> packets = SplitPackets.getSplitPackets().split(packet);
                for (byte[] p : packets) {
                    parsePacket(p);
                }
            }
        }
    }

    /**
     * Function to send a packet directly instead of creating packet. Used in
     * case of redirecting packets.
     *
     * @param packet the packet to send
     * @param destIp the list of destination to send the packet
     * @param serverIp the main server IP address details
     */
    public void sendPkt(final byte[] packet, final ClientNode[] destIp, final ClientNode serverIp) {
        for (ClientNode client : destIp) {
            communicator.sendData(packet, client);
            System.out.println("Sent data succesfully...");
        }
    }

    /**
     * Function to parse the received packet and perform required response.
     *
     * @param packet the packet to parse
     * @throws UnknownHostException throws when sending data to unknown host
     */
    public void parsePacket(final byte[] packet) {

        try {
            final PacketInfo pktInfo = parser.parsePacket(packet);
            final int module = pktInfo.getModule();
            final ModuleType type = moduleType.getType(module);
            final InetAddress address = pktInfo.getIpAddress();
            final int port = pktInfo.getPortNum();
            final String addr = address.getHostAddress();

            if (addr.equals(deviceIp) && port == devicePort) {
                final String data = new String(pktInfo.getPayload(), StandardCharsets.UTF_8);
                byte[] message = chunkManager.addChunk(packet);
                SimpleNetworkLogger.printInfo(MODULENAME, "Server Data length received : " + data.length());
                SimpleNetworkLogger.printInfo(MODULENAME, "Server Module received : " + type);
                if (message != null) {
                    final PacketInfo newpktInfo = parser.parsePacket(message);
                    message = newpktInfo.getPayload();
                    simpleNetworking.callSubscriber(message, type);
                }
            } else {
                final ClientNode dest = new ClientNode(address.getHostAddress(), port);
                SimpleNetworkLogger.printInfo(MODULENAME, "Redirecting data to : " + dest);
                final ClientNode[] dests = {dest};
                sendPkt(packet, dests, dest);
            }
        } catch (UnknownHostException ex) {
            SimpleNetworkLogger.printError(MODULENAME, "Server Could not parse packet succesfully...");
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
