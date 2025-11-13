package com.swe.networking;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Vector;

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

    /** The variable to store the client details. */
    private ClientNode user;

    /** The variable to store singleton chunk manager. */
    private final ChunkManager chunkManager;

    /** The variable to store singleton priority queue. */
    private final PriorityQueue priorityQueue;

    /** The variable to store singleton topology. */
    private final Topology topology;

    /** The variable to store singleton packet parser. */
    final private PacketParser parser;

    /** The variable to store maximum packet size to chunk. */
    private final int payloadSize = 10 * 1024; // 10 KB

    /** The variable thread to run start() method continuously. */
    Thread startThread = null;

    /**
     * Private constructor for Netwroking class.
     */
    private Networking() {
        chunkManager = ChunkManager.getChunkManager(payloadSize);
        priorityQueue = PriorityQueue.getPriorityQueue();
        topology = Topology.getTopology();
        parser = PacketParser.getPacketParser();
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
     * @param data     the data to be sent
     * @param dest     the dest to send to
     * @param module   the module to be sent to
     * @param priority the priority of the data
     */
    @Override
    public void sendData(final byte[] data, final ClientNode[] dest, final int module, final int priority) {
        final Vector<byte[]> chunks = getChunks(data, dest, module, priority, 0);
        for (byte[] chunk : chunks) {
            try {
                priorityQueue.addPacket(chunk);
                System.out.println("Sending chunk: " + chunk);
            } catch (UnknownHostException ex) {
                System.err.println("Unknown host exception: " + ex.getMessage());
            }
        }
    }

    /**
     * Function to continuously send data.
     */
    public void start() {
        while (!Thread.currentThread().isInterrupted()) {

            final byte[] packet = priorityQueue.nextPacket();
            if (packet == null) {
                continue;
            }
            try{
                final PacketInfo info = parser.parsePacket(packet);
                ClientNode dest = new ClientNode(info.getIpAddress().getHostName(), info.getPortNum());
                System.out.println(dest + " " +info.getIpAddress() + ": " + info.getPortNum());
                topology.sendPacket(packet, dest);

            }catch (Exception ex){
                System.out.println("Error while processing and sending packet: " + ex.getMessage());
            }
        }
        System.out.println("Networking is shutting down...");
    }

    /**
     * Function to chunk the given data by the chunk manager.
     *
     * @param data      the data to be sent
     * @param dest      the dest to send the packet
     * @param module    the module to be sent to
     * @param priority  the priority of the packet
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
//        pkt.setLength(payloadSize);

        Vector<byte[]> chunks = new Vector<>();
        for (ClientNode client : dest) {
            try {
                final int type = topology.getNetworkType(user, client);
                System.out.println(type + " " + client);
                pkt.setType(type);
                pkt.setIpAddress(InetAddress.getByName(client.hostName()));
                pkt.setPortNum(client.port());
                pkt.setConnectionType(NetworkConnectionType.MODULE.ordinal());
                chunks.addAll(chunkManager.chunk(pkt));
                System.out.println(chunks.size());
            } catch (UnknownHostException ex) {
            }
        }
        return chunks;
    }

    /**
     * Function to chunk the given data by the chunk manager to all clients.
     * here the dest does not matter
     *
     * @param data     the data to be sent
     * @param module   the module to be sent to
     * @param priority the priority of the packet
     */
    @Override
    public void broadcast(final byte[] data, final int module, final int priority) {
        final ClientNode[] dest = {topology.getServer(user) };
        final Vector<byte[]> chunks = getChunks(data, dest, module, priority, 1);
        for (byte[] chunk : chunks) {
            try {
                priorityQueue.addPacket(chunk);
                System.out.println("Sending chunk in broadcast: " + chunk);
            } catch (UnknownHostException ex) {
                System.err.println("Unknown host exception: " + ex.getMessage());
            }
        }
    }

    /**
     * Function that other modules subscribe to.
     *
     * @param name     the nameId of the module.
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

        if(startThread == null){
            startThread = new Thread(this::start);
            startThread.start();
        }
    }

    /**
     * Function to call the subscribed modules.
     *
     * @param module the module to call
     * @param data the data to sent
     */
    public void callSubscriber(final int module, final byte[] data) {
        final MessageListener function = listeners.get(module);
        if(function == null){
            System.out.println("No function found for module: " + module);
        }else {
            function.receiveData(data);
        }
    }

}
