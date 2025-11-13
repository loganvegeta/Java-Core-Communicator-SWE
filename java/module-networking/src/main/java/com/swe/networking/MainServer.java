package com.swe.networking;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * The mainserver across all clusters.
 */
public class MainServer implements P2PUser {

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
     * The port at which the TCP server runs.
     */
    private final int serverPort;

    /**
     * The thread to run the receive function.
     */
    private final Thread receiveThread;

    /**
     * variable to store the main cluster index.
     */
    private final int mainServerClusterIdx;

    /**
     * The timer object to monitor client timeouts.
     */
    private final Timer timer;

    /**
     * Variable to start the timer.
     */
    private final int timerTimeoutMilliSeconds = 5 * 1000;

    /**
     * Variable to store the server IP address.
     */
    private final ClientNode mainserver;

    /**
     * Variable to store the static serializer.
     */
    private final NetworkSerializer serializer;

    /**
     * Variable to store header size.
     */
    private final int packetHeaderSize = 22;

    /**
     * Variable to store chunk manager.
     */
    private final ChunkManager chunkManager;

    /**
     * Variable to store the chunksize.
     */
    private final int payloadSize = 10 * 1024;

    /**
     * Constructor function for the main server class.
     *
     * @param deviceAddress the IP address of the device
     * @param mainServerAddress the IP address of the main server
     */
    public MainServer(final ClientNode deviceAddress,
            final ClientNode mainServerAddress) {
        NetworkLogger.printInfo("MainServer", "Creating a new Main Server...");
        serverPort = deviceAddress.port();
        mainserver = mainServerAddress;
        mainServerClusterIdx = 0;
        serializer = NetworkSerializer.getNetworkSerializer();
        chunkManager = ChunkManager.getChunkManager(packetHeaderSize);
        timer = new Timer(timerTimeoutMilliSeconds, this::handleClientTimeout);
        NetworkLogger.printInfo("MainServer", "Listening at port:" + serverPort + " ...");
        communicator = new TCPCommunicator(serverPort);
        receiveThread = new Thread(() -> receive());
        receiveThread.start();
    }

    /**
     * Function to send to list of a destinations.
     *
     * @param data the data to be sent
     * @param destIp the destination to which the data is sent
     */
    @Override
    public void send(final byte[] data, final ClientNode[] destIp) {
        for (ClientNode dest : destIp) {
            NetworkLogger.printInfo("MainServer", "Sending data");
            final ClientNode sendDest = topology.getDestination(mainserver, dest);
            communicator.sendData(data, sendDest); // check of this should be dest
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
        NetworkLogger.printInfo("MainServer", "Sending data");
        final ClientNode sendDest = topology.getDestination(mainserver, destIp);
        communicator.sendData(data, sendDest); // check of this should be dest
    }

    /**
     * Function to receive the data from the sockets.
     */
    @Override
    public void receive() {
        while (true) {
            final byte[] packet = communicator.receiveData();
            if (packet != null) {
                final List<byte[]> packets = SplitPackets.getSplitPackets().split(packet);
                for (byte[] p : packets) {
                    parsePacket(p);
                }
                // parsePacket(packet);
            }
        }
    }

    /**
     * Function to parse the packet received and take necessary action.
     *
     * @param packet the packet received
     */
    private void parsePacket(final byte[] packet) {
        try {
            final int connectionType = parser.parsePacket(packet).getConnectionType();
            final int type = parser.parsePacket(packet).getType();
            final InetAddress destInet = parser.parsePacket(packet).getIpAddress();
            final String destinationIp = destInet.getHostAddress();
            final int destinationPort = parser.parsePacket(packet).getPortNum();
            final ClientNode dest = new ClientNode(destinationIp,
                    destinationPort);
            NetworkLogger.printInfo("MainServer", "Packet received from " + dest + " of type "
                    + type + " and connection type " + connectionType + "...");
            // check for broadcast packet
            if (parser.parsePacket(packet).getBroadcast() == 1) {
                for (ClientNode c : topology.getAllClusterServers()) {
                    if (!c.equals(mainserver) && !c.equals(dest)) {
                        send(packet, c);
                    }
                }
            }
            if (type == NetworkType.USE.ordinal()) {
                handleUsePacket(packet, dest, connectionType);
            } else if (type == NetworkType.OTHERCLUSTER.ordinal()) {
                // might new to change the type to SAMECLUSTER
                final ClientNode newDest = topology.getServer(dest);
                send(packet, newDest);
            } else if (type == NetworkType.SAMECLUSTER.ordinal()) {
                send(packet, dest);
            }
        } catch (UnknownHostException ex) {
        }
    }

    /**
     * Function to handle the use packet after receiving.
     *
     * @param packet the packet to be parsed
     * @param dest the destination from which the packet was received
     * @param connectionType the connection type of the packet
     */
    private void handleUsePacket(final byte[] packet, final ClientNode dest, final int connectionType) {
        try {
            if (connectionType == NetworkConnectionType.HELLO.ordinal()) {
                handleHello(dest);
            } else if (NetworkConnectionType.REMOVE.ordinal() == connectionType) {
                handleRemove(packet, dest);
            } else if (connectionType == NetworkConnectionType.ALIVE.ordinal()) {
                timer.updateTimeout(dest);
                NetworkLogger.printInfo("MainServer", "Received alive packet from " + dest);
            } else if (connectionType == NetworkConnectionType.MODULE.ordinal()) {
                NetworkLogger.printInfo("MainServer", "Passing to chunk manager...");
                final int module = parser.parsePacket(packet).getModule();
                final byte[] data = chunkManager.addChunk(packet);
                final Networking networking = Networking.getNetwork();
                if (data != null) {
                    networking.callSubscriber(module, data);
                }
            } else if (connectionType == NetworkConnectionType.CLOSE.ordinal()) {
                NetworkLogger.printInfo("MainServer", "Closing the Main Server");
            }
        } catch (UnknownHostException ex) {
        }
    }

    /**
     * Function add client to timer.
     *
     * @param client the client to be added.
     * @param idx the index of the cluster it belongs
     */
    private void addClientToTimer(final ClientNode client, final int idx) {
        if (idx == mainServerClusterIdx && !client.equals(mainserver)) {
            timer.addClient(client);
        } else if (topology.getAllClusterServers().contains(client)) {
            timer.addClient(client);
        }
    }

    /**
     * Function to send the network strcture to the requested client.
     *
     * @param dest the destination to send the packet
     */
    private void sendNetworkPktResponse(final ClientNode dest) {
        try {
            final NetworkStructure network = topology.getNetwork();
            final int randomFactor = (int) Math.pow(10, 6);
            final byte[] networkBytes = serializer.serializeNetworkStructure(network);
            final PacketInfo responsePacket = new PacketInfo();
            responsePacket.setLength(packetHeaderSize + networkBytes.length);
            responsePacket.setType(NetworkType.USE.ordinal());
            responsePacket.setPriority(0);
            responsePacket.setModule(ModuleType.NETWORKING.ordinal());
            responsePacket.setConnectionType(NetworkConnectionType.NETWORK.ordinal());
            responsePacket.setBroadcast(0);
            responsePacket.setIpAddress(InetAddress.getByName(dest.hostName()));
            responsePacket.setPortNum(dest.port());
            responsePacket.setMessageId((int) (Math.random() * randomFactor));
            responsePacket.setChunkNum(0);
            responsePacket.setChunkLength(1);
            responsePacket.setPayload(networkBytes);
            NetworkLogger.printInfo("MainServer", "Sending current network details...");
            NetworkLogger.printInfo("MainServer", network.toString()); // network is an object, convert to string
            final byte[] responsePkt = parser.createPkt(responsePacket);
            send(responsePkt, dest);
        } catch (UnknownHostException ex) {
        }
    }

    /**
     * Function to send add packet to the given destination.
     *
     * @param dest the new client to add
     * @param server the server to send to
     * @param clusterIdx the index of cluster it belongs to
     */
    private void sendAddPktResponse(final ClientNode dest, final ClientNode server, final int clusterIdx) {
        try {
            final int randomFactor = (int) Math.pow(10, 6);
            final ClientNetworkRecord addClient = new ClientNetworkRecord(dest, clusterIdx);
            final PacketInfo addPacket = new PacketInfo();
            final byte[] clientBytes = serializer.serializeClientNetworkRecord(addClient);
            addPacket.setLength(packetHeaderSize + clientBytes.length);
            addPacket.setType(NetworkType.USE.ordinal());
            addPacket.setPriority(0);
            addPacket.setModule(ModuleType.NETWORKING.ordinal());
            addPacket.setConnectionType(NetworkConnectionType.ADD.ordinal());
            addPacket.setBroadcast(0);
            addPacket.setIpAddress(InetAddress.getByName(server.hostName()));
            addPacket.setPortNum(server.port());
            addPacket.setMessageId((int) (Math.random() * randomFactor));
            addPacket.setChunkNum(0);
            addPacket.setChunkLength(1);
            addPacket.setPayload(clientBytes);
            final byte[] addPkt = parser.createPkt(addPacket);
            send(addPkt, server);
        } catch (UnknownHostException ex) {
        }
    }

    /**
     * Function to remove client from the network.
     *
     * @param packet the packe containing data
     * @param dest the destination to be removed
     * @throws UnknownHostException when host is unknown
     */
    private void handleRemove(final byte[] packet, final ClientNode dest) throws UnknownHostException {
        final PacketInfo packetInfo = parser.parsePacket(packet);
        final ClientNetworkRecord remClient = serializer.deserializeClientNetworkRecord(packetInfo.getPayload());
        topology.removeClient(remClient);
        if (remClient.clusterIndex() == topology.getClusterIndex(mainserver)) {
            timer.removeClient(dest);
        }
        final int myCluster = topology.getClusterIndex(mainserver);
        for (ClientNode c : topology.getClients(myCluster)) {
            if (c.equals(mainserver)) {
                continue;
            }
            send(packet, c);
        }
        for (ClientNode s : topology.getAllClusterServers()) {
            if (s.equals(mainserver)) {
                continue;
            }
            send(packet, s);
        }
        NetworkLogger.printInfo("MainServer", "Client " + remClient.client().hostName()
                + " removed from cluster"
                + remClient.clusterIndex());
    }

    /**
     * Function to handel hello packets.
     *
     * @param dest the destination to the network structure to
     */
    private void handleHello(final ClientNode dest) {
        NetworkLogger.printInfo("MainServer", "Responding " + dest + " with a Hello packet...");
        final int clusterIdx = topology.addClient(dest);
        addClientToTimer(dest, clusterIdx);
        sendNetworkPktResponse(dest);
        // send add packet to all cluster servers.
        final List<ClientNode> servers = topology.getAllClusterServers();
        for (ClientNode server : servers) {
            if (server.equals(mainserver) || server.equals(dest)) {
                continue;
            }
            sendAddPktResponse(dest, server, clusterIdx);
        }

        // send add packet to all cluster clients of this cluster
        final List<ClientNode> clients = topology.getClients(mainServerClusterIdx);
        for (ClientNode client : clients) {
            if (client.equals(mainserver)) {
                continue;
            }
            sendAddPktResponse(dest, client, clusterIdx);
        }
    }

    /**
     * Function to close the socekt of a client.
     *
     * @param client the client to close
     */
    public void closeClient(final ClientNode client) {
        timer.removeClient(client);
        communicator.closeSocket(client);
    }

    /**
     * Function to close the main server class.
     *
     */
    @Override
    public void close() {
        receiveThread.interrupt();
        communicator.close();
        timer.close();
    }

    /**
     * Function to handle timeout of clients.
     *
     * @param client the client which was timeout
     */
    private void handleClientTimeout(final ClientNode client) {
        try {
            NetworkLogger.printInfo("MainServer", "Reached timeout for client " + client + " ...");
            // send remove packet to all clients in the cluster and main server
            final ClientNetworkRecord remClient = new ClientNetworkRecord(client,
                    topology.getClusterIndex(client));
            topology.removeClient(remClient);
            timer.removeClient(client);
            final PacketInfo packetInfo = new PacketInfo();
            packetInfo.setLength(packetHeaderSize);
            packetInfo.setType(NetworkType.USE.ordinal());
            packetInfo.setConnectionType(NetworkConnectionType.REMOVE.ordinal());
            packetInfo.setPayload(client.toString().getBytes());
            // used when removing clients the destination does not matter here but give to
            // avoid error
            packetInfo.setIpAddress(InetAddress.getByName(client.hostName()));
            packetInfo.setPortNum(client.port());
            final byte[] removePacket = parser.createPkt(packetInfo);
            final List<ClientNode> servers = topology.getAllClusterServers();
            NetworkLogger.printInfo("MainServer", "servers " + servers);
            for (ClientNode server : servers) {
                if (server.equals(mainserver) || server.equals(client)) {
                    continue;
                }
                send(removePacket, server);
            }
            final List<ClientNode> clients = topology.getClients(mainServerClusterIdx);
            NetworkLogger.printInfo("MainServer", "clients " + clients);
            for (ClientNode newClient : clients) {
                if (newClient.equals(mainserver) || newClient.equals(client)) {
                    continue;
                }
                send(removePacket, newClient);
            }
            communicator.closeSocket(client);
        } catch (UnknownHostException ex) {
        }
    }
}
