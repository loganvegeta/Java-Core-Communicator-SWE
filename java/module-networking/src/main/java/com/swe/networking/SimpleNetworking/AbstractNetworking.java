package com.swe.networking.SimpleNetworking;

import com.swe.networking.ClientNode;
import com.swe.networking.ModuleType;

/**
 * Interface used between other modules and networking to send data.
 * Every module subscribes to this interface and then sends data
 * using the sendData function
 */
public interface AbstractNetworking {
    /**
     * Method to send data to given list of destination.
     *
     * @param data     the data to be sent
     * @param destIp   the list of destination to whom the data is sent
     * @param module   the destination module id
     * @param priority the priority of the send message
     */
    void sendData(byte[] data, ClientNode[] destIp,
            ModuleType module, int priority);

    /**
     * Method to subscribe to the networking module.
     *
     * @param name     the name of the module
     * @param function the function to invoke on receiving data
     */
    void subscribe(ModuleType name, MessageListener function);

    /**
     * Method to remove subscription.
     *
     * @param name the name of the module
     */
    void removeSubscription(ModuleType name);
}
