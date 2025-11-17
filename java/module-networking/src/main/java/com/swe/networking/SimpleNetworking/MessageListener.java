package com.swe.networking.SimpleNetworking;

/**
 * Interface which the networking module invokes during sending data.
 * Each module must implement their respective receiveData function
 *
 */
@FunctionalInterface
public interface MessageListener {
    /**
     * Method to invoke on receiving the data.
     *
     * @param data the data to be sent
     */
    void receiveData(byte[] data);
}
