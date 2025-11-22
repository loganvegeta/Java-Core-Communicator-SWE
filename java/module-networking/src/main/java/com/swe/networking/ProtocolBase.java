package com.swe.networking;

import java.nio.channels.SocketChannel;

/**
 * Common interface to send data using various protocols.
 */
public interface ProtocolBase {

    /**
     * Opens a socket in the current device.
     *
     * @return the created socket
     */
    SocketChannel openSocket();

    /**
     * Function to print all keys in a slector.
     */
    void printKeys();

    /**
     * To close an opened socket.
     *
     * @param client the client socket to close.
     */
    void closeSocket(ClientNode client);

    /**
     * To send data to given destination.
     *
     * @param data the data to be sent
     * @param dest the dest to send the data
     */
    void sendData(byte[] data, ClientNode dest);

    /**
     * To receive data/socket form clients.
     *
     * @return the received data
     */
    byte[] receiveData();

    /**
     * Function to handle socket closing at termination.
     */
    void close();
}
