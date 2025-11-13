package com.swe.networking;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * The main architecture of the networking module. Implements the cluster
 * networks
 */
public final class Topology implements AbstractTopology {

    /**
     * The List of all cluster clients.
     *
     */
    private final List<List<ClientNode>> clusters;
    /**
     * The List of all servers of all clusters.
     */
    private final List<ClientNode> clusterServers;
    /**
     * The total number of clusters.
     *
     */
    private int numClusters = 0;
    /**
     * The total number of clients.
     *
     */
    private int numClients;
    /**
     * The maximum size of a cluster.
     */
    private final int singleClusterSize = 6;
    /**
     * The variable to iterate through the clusters.
     */
    private int clusterIndex = 0;

    /**
     * Singleton design pattern to prevent repeating class instantiations.
     *
     */
    private static Topology topology = null;

    /**
     * The variable to store the user of the device.
     */
    private P2PUser user = null;

    private Topology() {
        clusters = new ArrayList<>();
        clusterServers = new ArrayList<>();
    }

    /**
     * Function to get the statically instantiated class object.
     *
     * @return Topology the statically instantiated class.
     */
    public static Topology getTopology() {
        if (topology == null) {
            System.out.println("Creating new Topology object...");
            topology = new Topology();
        }
        System.out.println("Passing already instantiated Topology object...");
        return topology;
    }

    /**
     * Function returns the cluster server in which the client is present. TODO
     * Update all the functions.
     *
     * @param dest The ip address of the destination client
     */
    @Override
    public ClientNode getServer(final ClientNode dest) {
        ClientNode node = null;
        for (int i = 0; i < numClusters; i++) {
            final List<ClientNode> clients = clusters.get(i);
            for (int j = 0; j < clients.size(); j++) {
                final ClientNode client = clients.get(j);
                if (client.equals(dest)) {
                    node = clusterServers.get(i);
                    return node;
                }
            }
        }
        return node;
    }

    /**
     * Add a user to the topology. Logic: choose a cluster, add the user, and
     * update bookkeeping.
     *
     * @param deviceAddress Ip address of the current device
     * @param mainServerAddress Ip address of the server device
     */
    public void addUser(final ClientNode deviceAddress,
            final ClientNode mainServerAddress) {
        // update the network and add the client
        if (deviceAddress.equals(mainServerAddress)) {
            System.out.println("This device is considered as the main Server");
            user = new MainServer(deviceAddress, mainServerAddress);
            final List<ClientNode> cluster = new ArrayList<>();
            cluster.add(deviceAddress);
            clusters.add(cluster);
            clusterServers.add(deviceAddress);
            numClusters = 1;
            numClients = 1;
        } else {
            try {
                user = new P2PCluster();
                ((P2PCluster) user).addUser(deviceAddress, mainServerAddress);
            } catch (UnknownHostException ex) {
                System.out.println("Error while adding user to the P2P cluster...");
            }
        }
    }

    /**
     * This function returns the current Network details.
     *
     * @return structure - The Devices connected to the current network
     */
    public NetworkStructure getNetwork() {
        final List<List<ClientNode>> clients = new ArrayList<>();
        final List<ClientNode> servers = new ArrayList<>();
        final NetworkStructure structure = new NetworkStructure(clients, servers);
        System.out.println(clusters);
        System.out.println(clusterServers);
        for (int i = 0; i < clusters.size(); i++) {
            structure.clusters().add(clusters.get(i));
            structure.servers().add(clusterServers.get(i));
        }
        return structure;
    }

    /**
     * Function to handle sockets while closing.
     */
    public void closeTopology() {
        user.close();
        System.out.println("Closing topology...");
    }

    /**
     * Function to add a client to the network.
     *
     * @param clientAddress the IP address details of the client.
     *
     * @return the index of cluster it is added to
     */
    public int addClient(final ClientNode clientAddress) {
        numClients += 1;

        final List<ClientNode> lastCluster = clusters.get(clusters.size() - 1);
        if (lastCluster.size() < singleClusterSize) {
            lastCluster.add(clientAddress);
            System.out.println("Added to cluster " + (numClusters - 1) + " ...");
            return numClusters - 1;
        } else {
            final List<ClientNode> cluster = new ArrayList<>();
            cluster.add(clientAddress);
            clusters.add(cluster);
            clusterServers.add(clientAddress);
            numClusters++;
            System.out.println("Adding to a new cluster...");
            return numClusters - 1;
        }
    }

    /**
     * Function to add a new client to the network.
     *
     * @param client the details of the new client
     */
    public void updateNetwork(final ClientNetworkRecord client) {
        final int idx = client.clusterIndex();
        final ClientNode newClient = client.client();
        clusters.get(idx).add(newClient);
    }

    /**
     * Function to remove a new client from the network.
     *
     * @param client the details of the client
     */
    public void removeClient(final ClientNetworkRecord client) {
        final int idx = client.clusterIndex();
        final ClientNode removeClient = client.client();
        clusters.get(idx).remove(removeClient);
        numClients -= 1;
        if (clusterServers.contains(removeClient)) {
            if (!clusters.get(idx).isEmpty()) {
                final ClientNode newServer = clusters.get(idx).get(0);
                clusterServers.set(idx, newServer);
                System.out.println("A new server has been decided\n");
                return;
            }
            clusters.remove(idx);
            clusterServers.remove(removeClient);
            System.out.println("Removed " + removeClient + "from the server list...");
            // numClusters -= 1;
        }
    }

    /**
     * Function to replace the current network with a new one.
     *
     * @param network the new network structure
     */
    public void replaceNetwork(final NetworkStructure network) {
        clusters.clear();
        clusterServers.clear();
        for (int i = 0; i < network.clusters().size(); i++) {
            clusters.add(network.clusters().get(i));
            clusterServers.add(network.servers().get(i));
        }
        numClusters = network.clusters().size();
        numClients = 0;
        for (List<ClientNode> cluster : clusters) {
            numClients += cluster.size();
        }
    }

    /**
     * Function to get the cluster index of a client.
     *
     * @param client the client whose index is needed
     * @return the cluster index of the client
     */
    public int getClusterIndex(final ClientNode client) {
        for (int i = 0; i < clusters.size(); i++) {
            if (clusters.get(i).contains(client)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Function to get all clients in a cluster.
     *
     * @param index the index of the cluster
     * @return list of all clients in the cluster
     */
    public List<ClientNode> getClients(final int index) {
        return clusters.get(index);
    }

    /**
     * Function to get all the cluster servers.
     *
     * @return list of all cluster servers.
     */
    public List<ClientNode> getAllClusterServers() {
        return clusterServers;
    }

    /**
     * Function to get all clients in current cluster.
     *
     * @return all the clients
     */
    public List<ClientNode> getAllClients() {
        final List<ClientNode> clients = new ArrayList<>();
        for (List<ClientNode> cluster : clusters) {
            for (ClientNode client : cluster) {
                clients.add(client);
            }
        }
        return clients;
    }

    /**
     * Function to get the network type based in source and destination.
     *
     * @param source the source IP address
     * @param dest the destination IP address
     * @return the type number
     */
    public int getNetworkType(final ClientNode source, final ClientNode dest) {
        final int srcClusterIdx = getClusterIndex(source);
        final int destClusterIdx = getClusterIndex(dest);
        if (srcClusterIdx == destClusterIdx) {
            return NetworkType.USE.ordinal();
        } else {
            return NetworkType.OTHERCLUSTER.ordinal();
        }
    }

    /**
     * Function to get the destination to send the packet to in the topology.
     *
     * @param source the source IP address
     * @param dest the destination IP address
     * @return the destination to send to
     */
    public ClientNode getDestination(final ClientNode source, final ClientNode dest) {
        final int srcClusterIdx = getClusterIndex(source);
        final int destClusterIdx = getClusterIndex(dest);
        if (srcClusterIdx == destClusterIdx) {
            return dest;
        } else {
            final ClientNode destServer = getServer(dest);
            return destServer;
        }
    }

    /**
     * Function to send the packet to underlying user.
     *
     * @param packet the packet to be send
     * @param dest the destination to send
     */
    public void sendPacket(final byte[] packet, final ClientNode dest) {
        user.send(packet, dest);
    }
}
