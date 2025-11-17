package com.swe.networking;

/**
 * Interface which the networking module invokes during sending data.
 * Each module must implement their respective receiveData function
 *
 */
@FunctionalInterface
public interface MessageListener {
    /**
     * Function to call on receiving data.
     *
     * @param data the data that is passed
     */
    void receiveData(byte[] data);
}
