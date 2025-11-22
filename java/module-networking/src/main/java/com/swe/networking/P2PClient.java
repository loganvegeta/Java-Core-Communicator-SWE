package com.swe.networking;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The Client belonging to a certain cluster.
 */
public class P2PClient implements P2PUser {

    /**
     * base Commumicator class to send , recieve and close.
     */
    private final ProtocolBase communicator;
    /**
     * get parser to decode packet.
     */
    private final PacketParser parser = PacketParser.getPacketParser();
    /**
     * network topology.
     */
    private final Topology topology = Topology.getTopology();
    /**
     * self client node.
     */
    private final ClientNode deviceAddress;
    /**
     * main server node.
     */
    private final ClientNode mainServerAddress;
    /**
     * cluster server node.
     */
    private ClientNode clusterServerAddress;

    /**
     * current running status.
     */
    private boolean running = true;
    /**
     * recieve thread.
     */
    private final Thread receiveThread;

    /**
     * alive thread manager.
     */
    private final ScheduledExecutorService aliveScheduler = null;

    /**
     * time interval gap to send alive packet.
     */
    private static final long ALIVE_INTERVAL_SECONDS = 2;

    /**
     * serializer.
     */
    private final NetworkSerializer serializer = NetworkSerializer.getNetworkSerializer();

    /** Variable to store header size. */
    private final int packetHeaderSize = 22;

    /** Variable to store chunk manager. */
    private final ChunkManager chunkManager;

    /**
     * Creates a new P2PClient.
     *
     * @param device The ClientNode info for this device.
     * @param server The ClientNode info for the mainServer.
     * @param tcpCommunicator the communicator for the communication.
     */
    public P2PClient(final ClientNode device, final ClientNode server, final ProtocolBase tcpCommunicator) {
        this.deviceAddress = device;
        this.mainServerAddress = server;

        this.communicator = tcpCommunicator;
        chunkManager = ChunkManager.getChunkManager(packetHeaderSize);

        updateClusterServer();
        // Starting the continuous receive loop
        this.receiveThread = new Thread(this::receive);
        this.receiveThread.setName("P2PClient-Receive-Thread");
        this.receiveThread.start();

        // start a scheduled ALIVE packets to the cluster server
//        this.aliveScheduler = Executors.newSingleThreadScheduledExecutor();
//        this.aliveScheduler.scheduleAtFixedRate(this::sendAlivePacket,
//                ALIVE_INTERVAL_SECONDS, ALIVE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void send(final byte[] data, final ClientNode[] destIp) {
        for (ClientNode dest : destIp) {

            final ClientNode sendDest = topology.getDestination(mainServerAddress, dest);
            System.out.println("p2pclient sending data to: " + sendDest);
            communicator.sendData(data, sendDest);
        }
        return;
    }

    @Override
    public void send(final byte[] data, final ClientNode destIp) {

        final ClientNode sendDest = topology.getDestination(mainServerAddress, destIp);
        System.out.println("p2pclient sending data to: " + sendDest);
        communicator.sendData(data, sendDest);
        return;
    }

    /**
     * Periodically sends an ALIVE (001) packet to this client's ClusterServer.
     */
    private void sendAlivePacket() {
        if (clusterServerAddress == null) {
            System.out.println("cluster server address is null");
        }

        try {
            System.out.println("p2pclient sending alive packet to: " + clusterServerAddress);
            final InetAddress selfIp = InetAddress.getByName(deviceAddress.hostName());
            final byte[] emptyPayload = new byte[0];

            final PacketInfo aliveInfo = new PacketInfo();
            aliveInfo.setLength(PacketParser.getHeaderSize());
            aliveInfo.setType(NetworkType.USE.ordinal());
            aliveInfo.setPriority(0);
            aliveInfo.setModule(ModuleType.NETWORKING.ordinal());
            aliveInfo.setConnectionType(NetworkConnectionType.ALIVE.ordinal());
            aliveInfo.setBroadcast(0);
            aliveInfo.setIpAddress(selfIp);
            aliveInfo.setPortNum(deviceAddress.port());
            aliveInfo.setMessageId(0);
            aliveInfo.setChunkNum(0);
            aliveInfo.setChunkLength(0);
            aliveInfo.setPayload(emptyPayload);

            final byte[] alivePacket = parser.createPkt(aliveInfo);

            communicator.sendData(alivePacket, clusterServerAddress);

        } catch (UnknownHostException e) {
            System.err.println("p2pclient failed to send alive packet");
        }
    }

    @Override
    public void receive() {
        while (running) {
            try {
                final byte[] packet = communicator.receiveData();
                if (packet == null) {
                    continue;
                }
                final List<byte[]> packets = SplitPackets.getSplitPackets().split(packet);
                for (byte[] p : packets) {
                    packetRedirection(p);
                }

            } catch (Exception e) {
                System.err.println("p2pclient received exception while processing packet");
            }
        }
    }

    /**
     * Main packet parsing logic based on the user's specification.
     *
     * @param packet The raw packet data.
     */
    private void packetRedirection(final byte[] packet) {
        System.out.println("p2pclient received packet from: " + deviceAddress.hostName());
        try {
            final PacketInfo info = parser.parsePacket(packet);
            if (info.getBroadcast() == 1) {
                info.setIpAddress(InetAddress.getByName(deviceAddress.hostName()));
                info.setPortNum(deviceAddress.port());
                final byte[] modifiedPacket = parser.createPkt(info);
                communicator.sendData(modifiedPacket, clusterServerAddress);
                return;
            }
            final int typeInt = info.getType();
            final NetworkType type = NetworkType.getType(typeInt);

            switch (type) {
                case CLUSTERSERVER:
                case SAMECLUSTER:
                case OTHERCLUSTER:
                    // dropping the packet
                    System.out.println("p2pclient received packet and dropping of type :" + type);
                    break;

                case USE:
                    parseUsePacket(info, packet);
                    break;
                default:
                    break;
            }
        } catch (UnknownHostException e) {
            System.err.println("p2pclient failed to parse packet IP: " + e.getMessage());
        }
    }

    /**
     * Handles Type 11 (USE) packets based on the connection type.
     *
     * @param info The raw packet data.
     * @param packet The raw packet data.
     */
    private void parseUsePacket(final PacketInfo info, final byte[] packet) throws UnknownHostException {
        final int connType = info.getConnectionType();
        final NetworkConnectionType connection = NetworkConnectionType.getType(connType);

        System.out.println("p2pclient received connection type: " + connection);
        switch (connection) {
            case HELLO: // 000 drop it only to be received by main server
            case ALIVE: // 001 drop it only to received by cluster server and main server

                System.out.println("p2pclient received HELLO or ALIVE packet");
                break;

            case ADD: // 010 : update the current network
                System.out.println("p2pclient received ADD packet : updating network structure");

                final ClientNetworkRecord newClient = serializer.deserializeClientNetworkRecord(info.getPayload());
                topology.updateNetwork(newClient);

                updateClusterServer();
                break;
            case REMOVE: // 011 : update the current network

                System.out.println("p2pclient received ADD or REMOVE packet");
                final ClientNetworkRecord oldClient = serializer.deserializeClientNetworkRecord(info.getPayload());
                topology.removeClient(oldClient);

                updateClusterServer();
                break;

            case NETWORK: // 100 : replace the current network

                System.out.println("p2pclient received NETWORK packet");
                final NetworkStructure newNetwork = serializer.deserializeNetworkStructure(info.getPayload());
                topology.replaceNetwork(newNetwork);

                updateClusterServer();
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

            case CLOSE: // 111 : close the client terminate

                System.out.println("p2pclient received CLOSE packet");
                close();
                break;

            default:
                System.err.println("p2pclient received unknown packet type");
        }
    }

    /**
     * this is helper function to update cluster server address.
     * may be changed after changing network structure (add, remove, replace)
     */

    void updateClusterServer() {
        System.out.println("p2pclient after updating server");
        this.clusterServerAddress = topology.getServer(this.deviceAddress);
        if (this.clusterServerAddress == null) {
            System.err.println("p2pclient: Not find my cluster server in topology.");
        }
        return;
    }

    @Override
    public void close() {
        if (!running) {
            return; // multiple closes
        }
        running = false;

        System.out.println("p2pclient started closing");

        // Stop sending ALIVE packets
        if (aliveScheduler != null) {
            aliveScheduler.shutdownNow();
        }

        // Close all network sockets
        if (communicator != null) {
            communicator.close();
        }


        // Stop the receive thread
        if (receiveThread != null) {
            receiveThread.interrupt();
        }

        System.out.println("p2pclient closed");
    }
}
