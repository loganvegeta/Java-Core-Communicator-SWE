package com.swe.networking;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Cluster class to store details of a given cluster.
 *
 */
public class P2PCluster implements P2PUser {

    /**
     * List of all clients belonging to the same cluster.
     */
    private List<ClientNode> clients;

    /**
     * The status of current device as client / server.
     */
    private P2PUser user;

    /**
     * The server details of the current cluster.
     */
    private ClientNode clusterServer;

    /**
     * To check if the current device is a server or not.
     */
    private boolean isServer = false;

    /**
     * Maximum packet size.
     */
    private static final int MAX_PACKET_SIZE = 65536;

    /**
     * Packet parser singleton object.
     */
    private static PacketParser packetParser = PacketParser.getPacketParser();

    /**
     * Constructor function for P2P Cluster class.
     *
     */
    private ProtocolBase tcpCommunicator;

    public P2PCluster() {
        System.out.println("Creating a new P2P Cluster...");
        clients = new ArrayList<>();
    }

    /**
     * Function to add current user to the network.
     *
     * @param client details of the current client
     * @param server details of the mainserver
     */
    public void addUser(final ClientNode client, final ClientNode server) throws UnknownHostException {
        System.out.println("Adding new user to the network...");
        // send hello to server
        final PacketInfo packetInfo = new PacketInfo();
        packetInfo.setLength(PacketParser.getHeaderSize());
        packetInfo.setType(NetworkType.USE.ordinal());
        packetInfo.setConnectionType(NetworkConnectionType.HELLO.ordinal());
        packetInfo.setIpAddress(InetAddress.getByName(client.hostName()));
        packetInfo.setPortNum(client.port());
        packetInfo.setPayload(new byte[0]);

        tcpCommunicator = new TCPCommunicator(client.port());

        final byte[] helloPacket = packetParser.createPkt(packetInfo);
        tcpCommunicator.sendData(helloPacket, server);
        clients.add(client);
        final Thread receiveThread = new Thread(this::receive);
        receiveThread.start();
        try {
            receiveThread.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        System.out.println("User added to the network...");
        final NetworkStructure networkStructure = Topology.getTopology().getNetwork();
        for (int i = 0; i < networkStructure.servers().size(); i++) {
            if (networkStructure.servers().get(i).equals(client)) {
                clusterServer = client;
                this.user = new P2PServer(client, server, tcpCommunicator);
                for (ClientNode c : networkStructure.clusters().get(i)) {
                    if (!c.equals(client)) {
                        ((P2PServer) this.user).monitor(c);
                    }
                }
                this.isServer = true;
                break;
            }
            if (networkStructure.clusters().get(i).contains(client)) {
                clusterServer = networkStructure.servers().get(i);
                this.isServer = false;
                user = new P2PClient(client, server);
                break;
            }
        }
    }

    /**
     * Function to resize the cluster dynamically.
     *
     * @param size The required size of cluster
     */
    public void resizeCluster(final Integer size) {
        if (clients.size() < size - 1) {
            System.out.println("Cannot resize cluster...");
        }
    }

    /**
     * Function to send data by the user.
     *
     * @param data the data to be sent
     * @param destIp the destinations to send the data
     */
    @Override
    public void send(final byte[] data, final ClientNode[] destIp) {
        this.user.send(data, destIp);
    }

    /**
     * Function to send data by the user.
     *
     * @param data the data to be sent
     * @param destIp the one destination to send the data
     */
    @Override
    public void send(final byte[] data, final ClientNode destIp) {
        this.user.send(data, destIp);
    }

    /**
     * Function to receive data from other clients.
     */
    @Override
    public void receive() {
        while (true) {
            final byte[] packet = tcpCommunicator.receiveData();
            if (packet == null) {
                continue;
            } else {
                try {
                    final PacketInfo packetInfo = packetParser.parsePacket(packet);
                    final NetworkStructure networkStructure = NetworkSerializer.getNetworkSerializer()
                            .deserializeNetworkStructure(packetInfo.getPayload());
                    Topology.getTopology().replaceNetwork(networkStructure);
                    break;
                } catch (Exception e) {
                    System.out.println("Error while receiving data in P2P Cluster...");
                }
            }
        }
    }

    /**
     * Function to handle socket closing at termination.
     */
    @Override
    public void close() {
        this.user.close();
    }

}
