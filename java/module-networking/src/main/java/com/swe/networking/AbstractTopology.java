package com.swe.networking;

/**
 * Interface used between Cluster and the topology class.
 *
 */
public interface AbstractTopology {
    /**
     * Function to get the cluster server.
     *
     * @param dest the destination to be sent to
     * @return the cluster server of the destination client belongs to
     */
    ClientNode getServer(ClientNode dest);
}
