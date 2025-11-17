package com.swe.networking;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import com.swe.core.RPCinteface.AbstractRPC;

/**
 * The main class of the networking module.
 */
public class Networking implements AbstractNetworking, AbstractController {

    /**
     * The singeton variable to store the class object.
     */
    private static Networking networking;
    /**
     * The variable to store all the listeners subscribed to the module.
     */
    private final HashMap<Integer, MessageListener> listeners = new HashMap<>();

    /**
     * The variable to store the client details.
     */
    private ClientNode user;

    /**
     * The variable to store singleton chunk manager.
     */
    private final ChunkManager chunkManager;

    /**
     * The variable to store singleton priority queue.
     */
    private PriorityQueue priorityQueue;
    /**
     * The variable to store singleton priority queue.
     */
    private PacketParser parser;

    /**
     * The variable to store singleton topology.
     */
    private final Topology topology;

    /**
     * The variable to store maximum packet size to chunk.
     */
    private final int payloadSize = 10 * 1024; // 10 KB

    /**
     * Variable to store the rpc for the app.
     */
    private AbstractRPC moduleRPC = null;
    /**
     * Variable to store the thread to start the send packets.
     */
    private final Thread sendThread;

    /**
     * Private constructor for Netwroking class.
     */
    private Networking() {
        chunkManager = ChunkManager.getChunkManager(payloadSize);
        priorityQueue = priorityQueue.getPriorityQueue();
        parser = PacketParser.getPacketParser();
        topology = Topology.getTopology();
        sendThread = new Thread(this::start);
        sendThread.start();
    }

    /**
     * Function to return the singleton Networking object.
     *
     * @return the singleton object
     */
    public static Networking getNetwork() {
        if (networking == null) {
            System.out.println("Instantiated Networking module...");
            networking = new Networking();
            return networking;
        }
        System.out.println("Already instantiated Networking module...");
        return networking;
    }

    /**
     * Function to chunk and send the recevied data to the queue.
     *
     * @param data the data to be sent
     * @param dest the dest to send to
     * @param module the module to be sent to
     * @param priority the priority of the data
     */
    @Override
    public void sendData(final byte[] data, final ClientNode[] dest, final int module, final int priority) {
        System.out.println("Data length : " + data.length);
        System.out.println("Destination : " + Arrays.toString(dest));
        final Vector<byte[]> chunks = getChunks(data, dest, module, priority, 0);
        System.out.println("chunk number : " + chunks.size());
        for (byte[] chunk : chunks) {
            try {
                final PacketInfo pktInfo = parser.parsePacket(chunk);
                final InetAddress addr = pktInfo.getIpAddress();
                final int port = pktInfo.getPortNum();
                // long startTime = System.currentTimeMillis();
                final ClientNode newdest = new ClientNode(addr.getHostAddress(), port);
                // long endTime = System.currentTimeMillis();
                // System.out.println("Time to create new dest: " + (endTime - startTime) + " ms");
                System.out.println("Destination " + newdest);
                topology.sendPacket(chunk, newdest);
//                priorityQueue.addPacket(chunk);
            } catch (UnknownHostException ex) {
            }
        }
    }

    /**
     * Function to continuously send data.
     */
    public void start() {
        while (true) {
            if (!priorityQueue.isEmpty()) {
                final byte[] packet = priorityQueue.nextPacket();
                try {
                    final PacketInfo pktInfo = parser.parsePacket(packet);
                    final InetAddress addr = pktInfo.getIpAddress();
                    final int port = pktInfo.getPortNum();
                    final ClientNode dest = new ClientNode(addr.getHostAddress(), port);
                    topology.sendPacket(packet, dest);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Function to chunk the given data by the chunk manager.
     *
     * @param data the data to be sent
     * @param dest the dest to send the packet
     * @param module the module to be sent to
     * @param priority the priority of the packet
     * @param broadcast the data should b broadcasted or not
     * @return the chunks of the data
     */
    private Vector<byte[]> getChunks(final byte[] data, final ClientNode[] dest, final int module, final int priority,
            final int broadcast) {
        final PacketInfo pkt = new PacketInfo();
        pkt.setModule(module);
        pkt.setPriority(priority);
        pkt.setBroadcast(broadcast);
        pkt.setPayload(data);
        Vector<byte[]> chunks = new Vector<>();
        for (ClientNode client : dest) {
            try {
//                final int type = topology.getNetworkType(user, client);
                final int type = 3;
                pkt.setType(type);
                pkt.setIpAddress(InetAddress.getByName(client.hostName()));
                pkt.setPortNum(client.port());
                pkt.setConnectionType(NetworkConnectionType.MODULE.ordinal());
                chunks = chunkManager.chunk(pkt);
            } catch (UnknownHostException ex) {
            }
        }
        return chunks;
    }

    /**
     * Function to chunk the given data by the chunk manager to all clients.
     * here the dest does not matter
     *
     * @param data the data to be sent
     * @param module the module to be sent to
     * @param priority the priority of the packet
     */
    @Override
    public void broadcast(final byte[] data, final int module, final int priority) {
        final ClientNode[] dest = {topology.getServer(user)};
        sendData(data, dest, module, priority);
    }

    /**
     * Function that other modules subscribe to.
     *
     * @param name the nameId of the module.
     * @param function the function to be called
     */
    @Override
    public void subscribe(final int name, final MessageListener function) {
        if (!listeners.containsKey(name)) {
            listeners.put(name, function);
            System.out.println("Added a new subscriber...");
            return;
        }
        System.out.println("The name already exist...");
    }

    /**
     * Function to remove Subscription from the networking.
     *
     * @param name the nameId of the module
     */
    @Override
    public void removeSubscription(final int name) {
        if (listeners.containsKey(name)) {
            listeners.remove(name);
            System.out.println("The module " + name + " is removed...");
            return;
        }
        System.out.println("The name doesnot exist...");
    }

    @Override
    public void addUser(final ClientNode deviceAddress, final ClientNode mainServerAddress) {
        user = deviceAddress;
        topology.addUser(deviceAddress, mainServerAddress);
    }

    /**
     * Function to call the subscirbed modules.
     *
     * @param module the module to call
     * @param data the data to sent
     */
    public void callSubscriber(final int module, final byte[] data) {
        final MessageListener function = listeners.get(module);
        function.receiveData(data);
    }

    /**
     * Function called to close the networking module.
     */
    @Override
    public void closeNetworking() {
        System.out.println("Closing Networking module...");
        topology.closeTopology();
    }

    /**
     * Function to consume the RPC.
     *
     * @param rpc the rpc to consume by the networking
     */
    @Override
    public void consumeRPC(final AbstractRPC rpc) {
        moduleRPC = rpc;
        final NetworkRPC networkRPC = NetworkRPC.getNetworkRPC();
        moduleRPC.subscribe("getNetworkRPCAddUser", networkRPC::networkRPCAddUser);
        moduleRPC.subscribe("networkRPCBroadcast", networkRPC::networkRPCBroadcast);
        moduleRPC.subscribe("networkRPCRemoveSubscription", networkRPC::networkRPCRemoveSubscription);
        moduleRPC.subscribe("networkRPCSendData", networkRPC::networkRPCSendData);
        moduleRPC.subscribe("networkRPCSubscribe", networkRPC::networkRPCSubscribe);
        moduleRPC.subscribe("networkRPCCloseNetworking", networkRPC::networkRPCCloseNetworking);
    }
}
