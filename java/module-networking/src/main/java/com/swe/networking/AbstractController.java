package com.swe.networking;

import com.swe.core.RPCinteface.AbstractRPC;

/**
 * The interface between the controller and networking modules.
 * Used to send the joining clients address to the networking module
 *
 */
public interface AbstractController {
    /**
     * Function to add user to the network.
     *
     * @param deviceAddress     the device IP address details
     * @param mainServerAddress the main server IP address details
     */
    void addUser(ClientNode deviceAddress, ClientNode mainServerAddress);

    /**
     * Method to close the networking module.
     */
    void closeNetworking();

    /**
     * Method to consume the RPC. This function attach all the handlers for the RPC methods to the networking module.
     * NO METHOD MAY BE ATTACHED TO THE RPC AFTER THIS FUNCTION IS CALLED.
     *
     * @param rpc the RPC to consume.
     */
    void consumeRPC(AbstractRPC rpc);
}
