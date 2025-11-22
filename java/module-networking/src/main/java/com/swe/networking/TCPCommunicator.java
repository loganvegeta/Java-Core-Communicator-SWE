package com.swe.networking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;

//File owned by Loganath
/**
 * Communicator class module for TCP.
 *
 */
public final class TCPCommunicator implements ProtocolBase {

    /**
     * Variable to store the name of the module.
     */
    private static final String MODULENAME = "[TCPCOMMUNICATOR]";
    /**
     * The server socket used to receive client connections.
     *
     */
    private ServerSocketChannel receiveSocket;
    /**
     * The selector for the sockets.
     */
    private Selector selector;

    /**
     * The list of all connected clients and their sockets.
     *
     */
    private final HashMap<ClientNode, SocketChannel> clientSockets = new HashMap<>();

    /**
     * The port where the server is instantiated.
     *
     */
    private Integer deviceServerPort = 0;
    // Integer clientSendTimeout = 2000;

    /**
     * The size of the buffer to read data. Let it be the size of 15KB.
     */
    private final Integer byteBufferSize = 15 * 1024;

    // maintain list of clients and add timeouts
    /**
     * Constructor function for TCP Communicator class.
     *
     * @param serverPort which port to start the TCP.
     */
    public TCPCommunicator(final int serverPort) {
        try {
            NetworkLogger.printInfo(MODULENAME, "TCP communicator initialized...");
            selector = Selector.open();
            deviceServerPort = serverPort;
            setServerPort();
        } catch (IOException ex) {
            // NetworkLogger.printError(MODULENAME, "Unable to initialize TCP comunicator...");
            // NetworkLogger.printError(MODULENAME, "Error : " + ex.getMessage());
        }
    }

    /**
     * Function to print all keys in the selector.
     */
    @Override
    public void printKeys() {
        selector.keys().stream().forEach(kay -> System.out.println(kay.channel()));
    }

    @Override
    public byte[] receiveData() {
        try {
            selector.select();
            final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                final SelectionKey key = iter.next();
                iter.remove();
                if (!key.isValid()) {
                    continue;
                }
                if (key.isAcceptable()) {
                    acceptConnection(key);
                } else if (key.isReadable()) {
                    final byte[] data = readData(key);
                    return data;
                }
            }
            return null;
        } catch (IOException ex) {
            // NetworkLogger.printError(MODULENAME, "Error while using the selector...");
            // NetworkLogger.printError(MODULENAME, "Error : " + ex.getMessage());
            return null;
        }
    }

    /**
     * Function to start a TCP socket at the given port.
     */
    private void setServerPort() {
        try {
            receiveSocket = ServerSocketChannel.open();
            receiveSocket.bind(new InetSocketAddress(deviceServerPort));
            receiveSocket.configureBlocking(false);
            receiveSocket.register(selector, SelectionKey.OP_ACCEPT);
            NetworkLogger.printInfo(MODULENAME, "Creating new server port at " + deviceServerPort);
        } catch (IOException ex) {
            NetworkLogger.printError(MODULENAME, "Error connecting to port : " + deviceServerPort);
            NetworkLogger.printError(MODULENAME, "Error : " + ex.getMessage());
        }
    }

    @Override
    public SocketChannel openSocket() {
        try {
            final SocketChannel socket = SocketChannel.open();
            return socket;
        } catch (IOException ex) {
            // NetworkLogger.printError(MODULENAME, "Error occurred while opening socket...");
            // NetworkLogger.printError(MODULENAME, "Error : " + ex.getMessage());
        }
        return null;
    }

    @Override
    public void closeSocket(final ClientNode client) {
        final SocketChannel clientSocket = clientSockets.get(client);
        if (clientSocket != null) {
            try {
                final SelectionKey key = clientSocket.keyFor(selector);
                if (key != null) {
                    System.out.println("Closing socket ");
                    key.cancel();
                    clientSocket.close();
                    clientSockets.remove(client);
                    NetworkLogger.printInfo(MODULENAME, "Closing socket for client " + client + " ...");
                }
            } catch (IOException ex) {
                // NetworkLogger.printError(MODULENAME, "Error occurred while closing socket...");
                // NetworkLogger.printError(MODULENAME, "Error : " + ex.getMessage());
            }
        }
    }

    @Override
    public void sendData(final byte[] data, final ClientNode dest) {
        final String destIp = dest.hostName();
        final Integer destPort = dest.port();
        try {
            final SocketChannel destSocket;
            if (clientSockets.containsKey(dest)) {
                NetworkLogger.printInfo(MODULENAME, "Connection to " + dest + " exists already...");
                destSocket = clientSockets.get(dest);
            } else {
                destSocket = openSocket();
                destSocket.configureBlocking(true);
                NetworkLogger.printInfo(MODULENAME, "Client : " + dest + " ...");
                destSocket.connect(new InetSocketAddress(destIp, destPort));
                destSocket.configureBlocking(false);
                destSocket.register(selector, SelectionKey.OP_READ);
                NetworkLogger.printInfo(MODULENAME, "Opening new socket at port " + destSocket.socket().getLocalPort());
                NetworkLogger.printInfo(MODULENAME, "New connection created successfully...");
                clientSockets.put(new ClientNode(destIp, destPort), destSocket);
            }
            final ByteBuffer buffer = ByteBuffer.wrap(data);
            while (buffer.hasRemaining()) {
                destSocket.write(buffer);
            }
            printIpAddr(destIp, destPort);
        } catch (IOException ex) {
            NetworkLogger.printError(MODULENAME, "Error while sending data...");
            NetworkLogger.printError(MODULENAME, "Error : " + ex.getMessage());
        }
    }

    /**
     * Function to accept new connection.
     *
     * @param key the selection key for new connections
     */
    public void acceptConnection(final SelectionKey key) {
        try {
            NetworkLogger.printInfo(MODULENAME, "Accepting new connection...");
            final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            final SocketChannel clientChannel = serverSocketChannel.accept();
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ);
            final String ip = clientChannel.getRemoteAddress().toString().split(":")[0].replace("/", "");
            final int port = ((InetSocketAddress) clientChannel.getRemoteAddress()).getPort();
            final ClientNode client = new ClientNode(ip, port);
            clientSockets.put(client, clientChannel);
            NetworkLogger.printInfo(MODULENAME, "New connection esthablished...");
            NetworkLogger.printInfo(MODULENAME, "Client " + client + " ...");
        } catch (IOException ex) {
            // NetworkLogger.printError(MODULENAME, "Error occured while accepting connection...");
            // NetworkLogger.printError(MODULENAME, "Error : " + ex.getMessage());
        }
    }

    /**
     * Function to read data from available socket.
     *
     * @param key the key for the socket
     * @return the data ead
     */
    public byte[] readData(final SelectionKey key) {
        try {
            final ByteBuffer buffer = ByteBuffer.allocate(byteBufferSize);
            final SocketChannel clientChannel = (SocketChannel) key.channel();
            final int bytesRead = clientChannel.read(buffer);
            if (bytesRead == -1) {
                return null;
            }
            NetworkLogger.printError(MODULENAME, "Bytes read : " + bytesRead);
            buffer.flip();
            final byte[] data = new byte[bytesRead];
            buffer.get(data);
            return data;
        } catch (IOException ex) {
            // NetworkLogger.printError(MODULENAME, "Error occured while reading data...");
            // NetworkLogger.printError(MODULENAME, "Error : " + ex.getMessage());
            return null;
        }
    }

    /**
     * Function to handle socket on termination.
     */
    @Override
    public void close() {
        try {
            NetworkLogger.printInfo(MODULENAME, "Closing TCP communicator...");
            receiveSocket.close();
            for (SocketChannel socket : clientSockets.values()) {
                NetworkLogger.printInfo(MODULENAME, "Closing socket of " + socket.getRemoteAddress() + "...");
                socket.close();
            }
        } catch (IOException ex) {
            // NetworkLogger.printError(MODULENAME, "Error occured while closing socket...");
            // NetworkLogger.printError(MODULENAME, "Error : " + ex.getMessage());
        }
    }

    private void printIpAddr(final String ipAddr, final Integer port) {
        NetworkLogger.printInfo(MODULENAME, "Client: " + ipAddr + ":" + port);
    }
}
