package com.swe.networking.SimpleNetworking;

import java.io.IOException;

import com.swe.networking.ClientNode;
import com.swe.networking.ModuleType;

/**
 * The interface used by various types of users to send and receive data.
 */
public interface IUser {
    /**
     * Method to send data to given list of destination.
     *
     * @param data     the data to be sent
     * @param destIp   the list of destination to whom the data is sent
     * @param serverIp the main server IP address
     * @param module   the destination module id
     */
    void send(byte[] data, ClientNode[] destIp,
            ClientNode serverIp, ModuleType module);

    /**
     * Method to receive data from socekts.
     *
     * @throws IOException throws on socket error
     */
    void receive() throws IOException;

    /**
     * Function to call on closing.
     */
    void closeUser();
}
