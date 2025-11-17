package com.swe.networking;

/**
 * Interface used between other modules and networking to send data.
 * Every module subscribes to this interface and then sends data
 * using the sendData function
 */
public interface AbstractNetworking {
    /**
     * Function to send data to given list of destination.
     *
     * @param data     the data to be sent
     * @param dest     the destination to send the data
     * @param module   the module to send to
     * @param priority the priority of the data
     */
    void sendData(byte[] data, ClientNode[] dest, int module, int priority);

    /**
     * Function to send data to all clients.
     *
     * @param data     the data to be sent
     * @param module   the module to be sent to
     * @param priority the priority of the data
     */
    void broadcast(byte[] data, int module, int priority);

    /**
     * Function to subscribe a function to the network.
     *
     * @param name     the name of the module.
     * @param function the function to invoke on receiving the packet
     */
    void subscribe(int name, MessageListener function);

    /**
     * Functiont to remove from subscription.
     *
     * @param name the name of the module
     */
    void removeSubscription(int name);

    
}
