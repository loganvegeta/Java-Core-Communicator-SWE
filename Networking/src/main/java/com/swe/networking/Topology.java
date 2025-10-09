package com.swe.networking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import static java.lang.Math.floor;
import static java.lang.Math.sqrt;

/**
 * The main architecture of the networking module.
 * Implements the cluster networks
 */
public class Topology implements AbstractTopology, AbstractController {
    /**
     * The hashmap of all clients present separated per cluster.
     */
    private HashMap<Cluster, ArrayList<ClientNode>> clientIP;
    /**
     * The collection of all cluster objects.
     *
     */
    private ArrayList<Cluster> clusters;
    /**
     * The total number of clusters.
     *
     */
    private int numClusters = 0;
    /**
     * The total number of clients.
     *
     */
    private int numClients = 0;


    public Topology() {
        clusters = new ArrayList<Cluster>();
        clientIP = new HashMap<>();
    }

    /**
     * Function returns the cluster server in which the client is present.
     *
     * @param dest The ip address of the destination client
     */
    @Override
    public ClientNode GetServer(final String dest) {
        ClientNode node = null;
        for (Cluster cluster : clusters) {
            final ArrayList<ClientNode> clientIPs = clientIP.get(cluster);
            int idx = -1;
            for (int i = 0; i < clientIPs.size(); i++) {
                if (Objects.equals(clientIPs.get(i).hostName(), dest)) {
                    idx = i;
                    node = clientIPs.get(i);
                }
            }
            if (idx == -1) {
                System.out.println("The client is not part of the network...");
                return null;
            }
            return node;
        }
        return null;
    }

    /**
     * Add a user to the topology.
     * Logic: choose a cluster, add the user, and update bookkeeping.
     *
     * @param ip   Destination IP
     * @param port Destination port
     */
    @Override
    public void addUser(final String ip, final Integer port) {
        final Cluster cluster = chooseCluster();
        cluster.addClient(ip, port);

        clientIP.computeIfAbsent(cluster, k -> new ArrayList<>())
                .add(new ClientNode(ip, port));
        numClients++;

        System.out.println("User added: "
                + ip + ":"
                + port
                + " -> Cluster#" + clusters.indexOf(cluster));
    }

    /**
     * Choose a cluster based on √N rule and least loaded cluster.
     * @return best chosen Cluster
     */
    private Cluster chooseCluster() {
        final int totalClients = numClients;
        final int maxClientPerCluster = (int) floor(sqrt(totalClients)) + 1;

        if (clusters.isEmpty()) {
            final Cluster newCluster = new Cluster();
            clusters.add(newCluster);
            clientIP.put(newCluster, new ArrayList<>());
            numClusters++;
            return newCluster;
        }

        // Find the least loaded cluster
        Cluster minCluster = clusters.get(0);
        int minSize = clientIP.getOrDefault(minCluster, new ArrayList<>()).size();

        for (Cluster candidate : clusters) {
            final int size = clientIP.getOrDefault(candidate, new ArrayList<>()).size();
            if (size < minSize) {
                minCluster = candidate;
                minSize = size;
            }
        }

        // Create new cluster if allowed and needed
        if (minSize >= maxClientPerCluster) {
            final Cluster newCluster = new Cluster();
            clusters.add(newCluster);
            clientIP.put(newCluster, new ArrayList<>());
            numClusters++;
            return newCluster;
        }

        return minCluster;
    }
}
