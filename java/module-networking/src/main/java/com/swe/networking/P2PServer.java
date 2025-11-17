package com.swe.networking;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * The server of a particular P2P Cluster.
 */
public class P2PServer implements P2PUser {

    /**
     * Communicator object to send and receive data.
     */
    private final ProtocolBase communicator;

    /**
     * Singleton packetparser object.
     */
    private final PacketParser parser = PacketParser.getPacketParser();

    /**
     * Singleton Topology object.
     */
    private final Topology topology = Topology.getTopology();

    /**
     * Singleton Serializer object.
     */
    private final NetworkSerializer serializer = NetworkSerializer.getNetworkSerializer();

    /**
     * The port at which the TCP server runs.
     */
    private final int serverPort;

    /**
     * The timer object to monitor client timeouts.
     */
    private final Timer timer;

    /**
     * The thread to run the receive function.
     */
    private final Thread receiveThread;

    /**
     * The thread to send ALIVE.
     */
    private final Thread sendThread = null;

    /**
     * The thread to monitor client timeouts.
     */
    private final int timerTimeout = 3000;

    /**
     * Threshold for client timeout in milliseconds.
     */
    private final int timeoutThreshold = 1000;

    /**
     * Variable to store header size.
     */
    private final int packetHeaderSize = 22;

    /**
     * Variable to store chunk manager.
     */
    private final ChunkManager chunkManager;

    /**
     * Main server Node.
     */
    private final ClientNode mainServer;

    /**
     * The p2pserver Node.
     */
    private final ClientNode deviceNode;

    /**
     * Constructor function for the cluster server class.
     *
     * @param deviceAddress the ClientNode of the device
     * @param mainServerAddress the IP address of the main server
     * @param tcpCommunicator the tcp communicator object to send messages
     */
    public P2PServer(final ClientNode deviceAddress,
            final ClientNode mainServerAddress, final ProtocolBase tcpCommunicator) {

        this.serverPort = deviceAddress.port();
        communicator = tcpCommunicator;
        chunkManager = ChunkManager.getChunkManager(packetHeaderSize);

        this.deviceNode = deviceAddress;
        this.mainServer = mainServerAddress;

        this.timer = new Timer(timerTimeout, this::handleClientTimeout);
//        sendThread = new Thread(this::sendAliveToMainServer);
        receiveThread = new Thread(this::receive);

//        sendThread.start();
        receiveThread.start();

        System.out.println("P2PServer");

        System.out.println("Created a new P2P Server at " + deviceAddress + "...");
    }

    /**
     * Function to send to list of destinations.
     *
     * @param data the data to be sent
     * @param destIp the destination to which the data is sent
     */
    @Override
    public void send(final byte[] data, final ClientNode[] destIp) {
        for (ClientNode dest : destIp) {
            System.out.println("Sending data to " + dest.hostName() + ":" + dest.port());
            communicator.sendData(data, dest);
        }
    }

    /**
     * Function to send to a single destination.
     *
     * @param data the data to be sent
     * @param destIp the destination to which the data is sent
     */
    @Override
    public void send(final byte[] data, final ClientNode destIp) {
        System.out.println("Sending data to " + destIp.hostName() + ":" + destIp.port());
        communicator.sendData(data, destIp);
    }

    /**
     * Function to receive the data from the sockets.
     */
    @Override
    public void receive() {
        while (true) {
            final byte[] packet = communicator.receiveData();
            if (packet == null) {
                continue;
            }
            try {
                final List<byte[]> packets = SplitPackets.getSplitPackets().split(packet);
                for (byte[] p : packets) {
                    handlePacket(p);
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Function to handle the received packet.
     *
     * @param packet the received packet
     * @throws UnknownHostException if the ip address is invalid
     */
    private void handlePacket(final byte[] packet) throws UnknownHostException {
        // parse the packet
        final PacketInfo packetInfo = parser.parsePacket(packet);
        final int connectionType = packetInfo.getConnectionType();
        final int type = packetInfo.getType();
        final InetAddress destInet = packetInfo.getIpAddress();
        final String destinationIp = destInet.getHostAddress();
        final int destinationPort = packetInfo.getPortNum();
        final ClientNode dest = new ClientNode(destinationIp,
                destinationPort);

        // check for broadcast
        if (packetInfo.getBroadcast() == 1) {
            System.out.println("Broadcast packet received at P2PServer.");
            // modify packet to dest as self
            packetInfo.setIpAddress(InetAddress.getByName(deviceNode.hostName()));
            packetInfo.setPortNum(deviceNode.port());
            final byte[] newPacket = parser.createPkt(packetInfo);
            handleBroadcast(newPacket, dest);
            return;
        }

        // handle based on type and connection type
        if (type == NetworkType.USE.ordinal() || type == NetworkType.CLUSTERSERVER.ordinal()) {
            handleUsePacket(connectionType, packet, dest);
        } else if (type == NetworkType.SAMECLUSTER.ordinal()) {
            final PacketInfo pktInfo = parser.parsePacket(packet);
            pktInfo.setType(NetworkType.USE.ordinal());
            final byte[] newPacket = parser.createPkt(pktInfo);
            send(newPacket, dest);
        } else if (type == NetworkType.OTHERCLUSTER.ordinal()) {
            // TOD change the type to appropriate one
            final ClientNode clusterServer = topology.getServer(dest);
            send(packet, clusterServer);
        } else {
            System.out.println("Unknown packet type received.");
        }
    }

    /**
     * Function to handle broadcasting a packet.
     *
     * @param newPacket the packet to be broacasted
     * @param dest the destination from which the packet was received
     */
    private void handleBroadcast(final byte[] newPacket, final ClientNode dest) {
        for (ClientNode c : topology.getClients(topology.getClusterIndex(deviceNode))) {
            if (c.equals(deviceNode) || c.equals(dest)) {
                continue;
            }
            send(newPacket, c);
        }
        for (ClientNode servers : topology.getAllClusterServers()) {
            if (servers.equals(deviceNode) || servers.equals(dest)) {
                continue;
            }
            send(newPacket, servers);
        }
    }

    /**
     * Handle packets whose NetworkType is USE or CLUSTERSERVER.
     *
     * @param connectionType the connection type of the packet
     * @param packet the received packet
     * @param dest the destination client node
     */
    private void handleUsePacket(final int connectionType, final byte[] packet,
            final ClientNode dest) {
        final NetworkConnectionType conn = NetworkConnectionType.values()[connectionType];
        try {
            switch (conn) {
                case HELLO:
                    System.out.println("HELLO packet received, not supported by P2PServer.");
                    break;
                case ALIVE:
                    timer.updateTimeout(dest);
                    System.out.println("ALIVE packet received from " + dest.hostName() + ".");
                    break;
                case ADD:
                    handleAdd(packet, dest);
                    break;
                case REMOVE:
                    handleRemove(packet, dest);
                    break;
                case NETWORK:
                    handleNetwork(packet);
                    break;
                case MODULE:
                    System.out.println("MODULE packet received");
                    final int module = parser.parsePacket(packet).getModule();
                    final byte[] data = chunkManager.addChunk(packet);
                    final Networking networking = Networking.getNetwork();
                    if (data != null) {
                        final PacketInfo destpktInfo = parser.parsePacket(data);
                        networking.callSubscriber(module, destpktInfo.getPayload());
                    }
                    break;
                case CLOSE:
                    close();
                    break;
                default:
                    System.out.println("Unknown connection type received.");
                    break;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private void handleAdd(final byte[] packet, final ClientNode dest) throws UnknownHostException {
        final PacketInfo packetInfo = parser.parsePacket(packet);
        final ClientNetworkRecord client = serializer.deserializeClientNetworkRecord(packetInfo.getPayload());
        topology.updateNetwork(client);
        if (client.clusterIndex() == topology.getClusterIndex(deviceNode)) {
            timer.addClient(dest);
            System.out.println("Client " + client.client().hostName()
                    + " added to timer.");
        }
        final int myCluster = topology.getClusterIndex(deviceNode);
        for (ClientNode c : topology.getClients(myCluster)) {
            if (c.equals(deviceNode)) {
                continue;
            }
            send(packet, c);
        }
        System.out.println("Client " + client.client().hostName()
                + " added to cluster"
                + client.clusterIndex());
    }

    private void handleRemove(final byte[] packet, final ClientNode dest) throws UnknownHostException {
        final PacketInfo packetInfo = parser.parsePacket(packet);
        final ClientNetworkRecord remClient = serializer.deserializeClientNetworkRecord(packetInfo.getPayload());
        topology.removeClient(remClient);
        if (remClient.clusterIndex() == topology.getClusterIndex(deviceNode)) {
            timer.removeClient(dest);
        }
        final int myCluster = topology.getClusterIndex(deviceNode);
        for (ClientNode c : topology.getClients(myCluster)) {
            if (c.equals(deviceNode)) {
                continue;
            }
            send(packet, c);
        }
        System.out.println("Client " + remClient.client().hostName()
                + " removed from cluster"
                + remClient.clusterIndex());
    }

    private void handleNetwork(final byte[] packet) throws UnknownHostException {
        final PacketInfo packetInfo = parser.parsePacket(packet);
        final NetworkStructure network = serializer.deserializeNetworkStructure(packetInfo.getPayload());
        topology.replaceNetwork(network);
        System.out.println("Network structure updated at server.");
    }

    /**
     * Function to send ALIVE packets to main server.
     *
     */
    private void sendAliveToMainServer() {
        final PacketInfo packetInfo = new PacketInfo();
        packetInfo.setLength(PacketParser.getHeaderSize());
        packetInfo.setType(NetworkType.USE.ordinal());
        packetInfo.setConnectionType(NetworkConnectionType.ALIVE.ordinal());
        packetInfo.setPayload(new byte[0]);
        try {
            packetInfo.setIpAddress(InetAddress.getByName(deviceNode.hostName()));
        } catch (UnknownHostException e) {
            System.out.println("Unknown host: " + deviceNode.hostName());
            e.printStackTrace();
            return;
        }
        packetInfo.setPortNum(deviceNode.port());
        final byte[] alivePacket = parser.createPkt(packetInfo);
        while (true) {
            System.out.println("Sending ALIVE to main server...");
            send(alivePacket, mainServer);
            try {
                Thread.sleep(timeoutThreshold);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Function to handle client timeout.
     *
     * @param client the client that has timed out
     */
    private void handleClientTimeout(final ClientNode client) {
        // send remove packet to all clients in the cluster and main server
        final ClientNetworkRecord remClient = new ClientNetworkRecord(client,
                topology.getClusterIndex(client));
        topology.removeClient(remClient);
        timer.removeClient(client);
        final PacketInfo packetInfo = new PacketInfo();
        packetInfo.setType(NetworkType.USE.ordinal());
        packetInfo.setConnectionType(NetworkConnectionType.REMOVE.ordinal());
        packetInfo.setPayload(client.toString().getBytes());
        packetInfo.setLength(PacketParser.getHeaderSize() + packetInfo.getPayload().length);
        final byte[] removePacket = parser.createPkt(packetInfo);
        send(removePacket, mainServer);
        for (ClientNode c : topology.getClients(topology.getClusterIndex(deviceNode))) {
            send(removePacket, c);
        }
        communicator.closeSocket(client);
    }

    /**
     * Function to monitor a new client.
     *
     * @param client the client to monitor
     */
    public void monitor(final ClientNode client) {
        timer.addClient(client);
    }

    /**
     * Function to handle socket closing at termination.
     */
    @Override
    public void close() {
        communicator.close();
        receiveThread.interrupt();
        sendThread.interrupt();
        timer.close();
    }

}
