package com.swe.networking;

/**
 * Implement
 * > A simple transferPacket which receives packet from the
 * other cluster servers and send it to the respective cluster
 *
 */


public interface AbstractTopology {
    ClientNode GetServer(String dest);
    void addUser(final String ip, final Integer port);
}
