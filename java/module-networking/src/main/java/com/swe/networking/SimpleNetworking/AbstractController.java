package com.swe.networking.SimpleNetworking;

import com.swe.core.RPCinteface.AbstractRPC;
import com.swe.networking.ClientNode;

/**
 * Interface to interact between controller and networking.
 */
public interface AbstractController {
    /**
     * Method to add user details to the network.
     *
     * @param deviceAddress     the device IP address details.
     * @param mainServerAddress the main server IP address details.
     */
    void addUser(ClientNode deviceAddress, ClientNode mainServerAddress);

    /**
     * Method to close the networking module.
     */
    void closeNetworking();

    /**
     * Method to consume the RPC. This function must attach 
     * all the handlers for the RPC methods to the networking module.
     * NO METHOD MAY BE ATTACHED TO THE RPC AFTER THIS FUNCTION IS CALLED.
     *
     * @param rpc the RPC to consume.
     */
    void consumeRPC(AbstractRPC rpc);
}

