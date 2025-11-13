package com.swe.networking;

import java.util.ArrayList;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Test class for MainServer.
 */
public class MainServerTest {
    /**
     * Function to test send Function.
     */
    @org.junit.jupiter.api.Test
    public void testSend() {
        try {
            final int serverport1 = 8001;
            final int serverport2 = 8002;
            final int serverport3 = 8003;
            final int serverport4 = 8004;
            final Thread recieveThread1 = new Thread(() -> receive(serverport1));
            final Thread recieveThread2 = new Thread(() -> receive(serverport2));
            final Thread recieveThread3 = new Thread(() -> receive(serverport3));
            final Thread recieveThread4 = new Thread(() -> receive(serverport4));
            recieveThread1.start();
            recieveThread2.start();
            recieveThread3.start();
            recieveThread4.start();
            final Integer sleepTime = 500;
            Thread.sleep(sleepTime);
            final ClientNode server = new ClientNode("127.0.0.1", 8000);
            final MainServer mainServer = new MainServer(server, server);
            final ClientNode dest1 = new ClientNode("127.0.0.1", 8001);
            final ClientNode dest2 = new ClientNode("127.0.0.1", 8002);
            final String data = "Good morning !!!";
            mainServer.send(data.getBytes(), dest1);
            mainServer.send(data.getBytes(), dest1);
            mainServer.send(data.getBytes(), dest1);
            mainServer.send(data.getBytes(), dest2);
            mainServer.closeClient(dest1);
            mainServer.closeClient(dest2);
            Thread.sleep(sleepTime);
            final ClientNode dest3 = new ClientNode("127.0.0.1", 8003);
            final ClientNode dest4 = new ClientNode("127.0.0.1", 8004);
            final ClientNode[] clients = { dest3, dest4 };
            mainServer.send(data.getBytes(), clients);
            mainServer.closeClient(dest3);
            mainServer.closeClient(dest4);
            recieveThread1.join();
            recieveThread2.join();
            recieveThread3.join();
            recieveThread4.join();
            mainServer.close();
        } catch (InterruptedException ex) {
        }
    }

    /**
     * Test function to check send is working.
     * 
     * @param serverPort to set the port of server
     */
    public void receive(final int serverPort) {
        try {
            final ServerSocket serverSocket = new ServerSocket(serverPort);
            serverSocket.setSoTimeout(0);
            final Socket socket = serverSocket.accept();
            final DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            final byte[] packet = dataIn.readAllBytes();
            System.out.println("Client port:" + serverPort + " data : " + new String(packet));
            System.out.println("Data received successfully...");
            serverSocket.close();
        } catch (IOException ex) {
        }
    }

    /**
     * Function to test the receive functionality.
     */
    @org.junit.jupiter.api.Test
    public void testReceive() {
        try {
            final int sleepTime = 1000;
            final ClientNode server = new ClientNode("127.0.0.1", 8000);
            final MainServer mainServer = new MainServer(server, server);
            send();
            Thread.sleep(sleepTime);
            mainServer.close();
        } catch (InterruptedException ex) {
        }
    }

    /**
     * Test function to send data.
     */
    public void send() {
        try {
            final Socket destSocket = new Socket();
            final Integer port = 8000;
            final Integer timeout = 5000;
            destSocket.connect(new InetSocketAddress("127.0.0.1", port), timeout);
            final DataOutputStream dataOut = new DataOutputStream(destSocket.getOutputStream());
            final ClientNode client = new ClientNode("127.0.0.1", destSocket.getLocalPort());
            final byte[] packet = getHelloPacket(client);
            dataOut.write(packet);
            destSocket.close();
        } catch (IOException ex) {
        }
    }

    public byte[] getRemovePacket(final ClientNode client) {
        try {
            final Topology topology = Topology.getTopology();
            final int randomFactor = (int) Math.pow(10, 6);
            final PacketParser parser = PacketParser.getPacketParser();
            final PacketInfo pkt = new PacketInfo();
            final int packetHeaderSize = 22;
            pkt.setLength(packetHeaderSize);
            pkt.setType(NetworkType.USE.ordinal());
            pkt.setPriority(0);
            pkt.setModule(0);
            pkt.setConnectionType(NetworkConnectionType.REMOVE.ordinal());
            pkt.setBroadcast(0);
            pkt.setIpAddress(InetAddress.getByName(client.hostName()));
            pkt.setPortNum(client.port());
            pkt.setMessageId((int) (Math.random() * randomFactor));
            pkt.setChunkNum(0);
            pkt.setChunkLength(1);
            final int idx = topology.getClusterIndex(client);
            final ClientNetworkRecord record = new ClientNetworkRecord(client, idx);
            pkt.setPayload(NetworkSerializer.getNetworkSerializer().serializeClientNetworkRecord(record));
            final byte[] packet = parser.createPkt(pkt);
            return packet;
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    public byte[] getHelloPacket(final ClientNode client) {
        try {
            final int randomFactor = (int) Math.pow(10, 6);
            final PacketParser parser = PacketParser.getPacketParser();
            final PacketInfo pkt = new PacketInfo();
            final int packetHeaderSize = 22;
            final String data = "Hello Sekai !!!";
            pkt.setLength(packetHeaderSize + data.length());
            pkt.setType(NetworkType.USE.ordinal());
            pkt.setPriority(0);
            pkt.setModule(0);
            pkt.setConnectionType(NetworkConnectionType.HELLO.ordinal());
            pkt.setBroadcast(0);
            pkt.setIpAddress(InetAddress.getByName(client.hostName()));
            pkt.setPortNum(client.port());
            pkt.setMessageId((int) (Math.random() * randomFactor));
            pkt.setChunkNum(0);
            pkt.setChunkLength(1);
            pkt.setPayload(data.getBytes());
            final byte[] packet = parser.createPkt(pkt);
            return packet;
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    @org.junit.jupiter.api.Test
    public void testMainServerTimeout() {
        final ClientNode server = new ClientNode("127.0.0.1", 8000);
        final MainServer mainServer = new MainServer(server, server);
        timeoutsend();
    }

    public void timeoutsend() {
        try {
            final Socket destSocket = new Socket();
            final Integer port = 8000;
            final Integer timeout = 5000;
            destSocket.connect(new InetSocketAddress("127.0.0.1", port), timeout);
            final DataOutputStream dataOut = new DataOutputStream(destSocket.getOutputStream());
            final ClientNode client = new ClientNode("127.0.0.1", destSocket.getLocalPort());
            final byte[] packet = getHelloPacket(client);
            dataOut.write(packet);
            Thread.sleep(6000);
        } catch (IOException | InterruptedException ex) {
        }
    }

    /**
     * A receiver method that listens on a port, prints the received message,
     * and can be safely interrupted by closing the socket.
     * @param port The port number to listen on.
     * @param serverSocketToClose The ServerSocket to close for shutdown.
     */
    public void stoppableReceive(final int port, ServerSocket[] serverSocketToClose) {
        try {
            final ServerSocket serverSocket = new ServerSocket(port);
            serverSocketToClose[0] = serverSocket; // Store the socket
            serverSocket.setSoTimeout(5000); // Set a timeout to prevent indefinite blocking

            final Socket socket = serverSocket.accept();
            final DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            final byte[] packet = dataIn.readAllBytes();
            System.out.println("Client port:" + port + " data : " + new String(packet));
            System.out.println("Data received successfully...");
            socket.close();
            serverSocket.close();
        } catch (IOException ex) {
            System.out.println("Receiver on port " + port + " was interrupted or timed out.");
        }
    }

    /**
     * Tests routing functionality.
     */
    @org.junit.jupiter.api.Test
    public void testRouting() {
        System.out.println("Testing routing...");
        final int mainServerPort = 8020;
        final int otherServerPort = 8021;
        final int clientPort = 9020;
        final ServerSocket[] otherServerSock = new ServerSocket[1];
        MainServer mainServer = null;
        Thread otherServerThread = null;

        try {
            // Setup
            final ClientNode mainServerNode = new ClientNode("127.0.0.1", mainServerPort);
            final ClientNode otherServerNode = new ClientNode("127.0.0.1", otherServerPort);
            final ClientNode clientNode = new ClientNode("127.0.0.1", clientPort);

            // Reset and manually set up topology
            Topology.getTopology().replaceNetwork(new NetworkStructure(new ArrayList<>(), new ArrayList<>()));
            Topology.getTopology().addClient(mainServerNode);
            Topology.getTopology().addClient(otherServerNode);
            Topology.getTopology().addClient(clientNode);

            // Start a listener for the "other server"
            otherServerThread = new Thread(() -> stoppableReceive(otherServerPort, otherServerSock));
            otherServerThread.start();

            // Start the main server
            mainServer = new MainServer(mainServerNode, mainServerNode);
            Thread.sleep(500); // Give server time to start

            // Create and send a packet destined for a client in another cluster
            PacketInfo packetInfo = new PacketInfo();
            packetInfo.setLength(22);
            packetInfo.setType(NetworkType.OTHERCLUSTER.ordinal());
            packetInfo.setConnectionType(NetworkConnectionType.HELLO.ordinal());
            packetInfo.setIpAddress(InetAddress.getByName(clientNode.hostName()));
            packetInfo.setPortNum(clientNode.port());
            packetInfo.setPayload("Routing Test".getBytes());
            byte[] packet = PacketParser.getPacketParser().createPkt(packetInfo);

            // Send packet to main server, which should route it to otherServerNode
            try (Socket socket = new Socket(mainServerNode.hostName(), mainServerNode.port())) {
                DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
                dataOut.write(packet);
                dataOut.flush();
            }

            otherServerThread.join(2000); // Wait for the thread to finish, with a timeout

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Teardown
            if (mainServer != null) {
                mainServer.close();
            }
            if (otherServerSock[0] != null && !otherServerSock[0].isClosed()) {
                try {
                    otherServerSock[0].close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (otherServerThread != null && otherServerThread.isAlive()) {
                otherServerThread.interrupt();
            }
            System.out.println("Finished routing test.");
        }
    }

    /**
     * Tests broadcast functionality.
     */
    @org.junit.jupiter.api.Test
    public void testBroadcast() {
        System.out.println("Testing broadcast...");
        final int mainServerPort = 8030;
        final int serverBPort = 8031;
        final int serverCPort = 8032;
        final ServerSocket[] serverBSock = new ServerSocket[1];
        final ServerSocket[] serverCSock = new ServerSocket[1];
        MainServer mainServer = null;
        Thread serverBThread = null;
        Thread serverCThread = null;

        try {
            // Setup
            final ClientNode mainServerNode = new ClientNode("127.0.0.1", mainServerPort);
            final ClientNode serverBNode = new ClientNode("127.0.0.1", serverBPort);
            final ClientNode serverCNode = new ClientNode("127.0.0.1", serverCPort);

            // Reset and manually set up topology
            Topology.getTopology().replaceNetwork(new NetworkStructure(new ArrayList<>(), new ArrayList<>()));
            Topology.getTopology().addClient(mainServerNode);
            Topology.getTopology().addClient(serverBNode);
            Topology.getTopology().addClient(serverCNode);

            // Start listeners for other servers
            serverBThread = new Thread(() -> stoppableReceive(serverBPort, serverBSock));
            serverCThread = new Thread(() -> stoppableReceive(serverCPort, serverCSock));
            serverBThread.start();
            serverCThread.start();

            // Start the main server
            mainServer = new MainServer(mainServerNode, mainServerNode);
            Thread.sleep(500);

            // Create and send a broadcast packet
            PacketInfo packetInfo = new PacketInfo();
            packetInfo.setLength(22);
            packetInfo.setType(NetworkType.USE.ordinal());
            packetInfo.setConnectionType(NetworkConnectionType.HELLO.ordinal());
            packetInfo.setIpAddress(InetAddress.getByName(mainServerNode.hostName()));
            packetInfo.setPortNum(mainServerNode.port());
            packetInfo.setBroadcast(1); // Set broadcast flag
            packetInfo.setPayload("Broadcast Test".getBytes());
            byte[] packet = PacketParser.getPacketParser().createPkt(packetInfo);

            // Send packet to main server
            try (Socket socket = new Socket(mainServerNode.hostName(), mainServerNode.port())) {
                DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
                dataOut.write(packet);
                dataOut.flush();
            }

            serverBThread.join(2000);
            serverCThread.join(2000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Teardown
            if (mainServer != null) {
                mainServer.close();
            }
            // Close server sockets
            for (ServerSocket sock : new ServerSocket[]{serverBSock[0], serverCSock[0]}) {
                if (sock != null && !sock.isClosed()) {
                    try {
                        sock.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            // Interrupt threads if still alive
            for (Thread t : new Thread[]{serverBThread, serverCThread}) {
                if (t != null && t.isAlive()) {
                    t.interrupt();
                }
            }
            System.out.println("Finished broadcast test.");
        }
    }

    /**
     * Tests routing to a client within the same cluster.
     */
    @org.junit.jupiter.api.Test
    public void testSameClusterRouting() {
        System.out.println("Testing same-cluster routing...");
        final int mainServerPort = 8040;
        final int clientPort = 9040;
        final ServerSocket[] clientSock = new ServerSocket[1];
        MainServer mainServer = null;
        Thread clientThread = null;

        try {
            // Setup
            final ClientNode mainServerNode = new ClientNode("127.0.0.1", mainServerPort);
            final ClientNode clientNode = new ClientNode("127.0.0.1", clientPort);

            // Reset and manually set up topology
            Topology.getTopology().replaceNetwork(new NetworkStructure(new ArrayList<>(), new ArrayList<>()));
            Topology.getTopology().addClient(mainServerNode);
            Topology.getTopology().addClient(clientNode); // Client in the same cluster

            // Start a listener for the client
            clientThread = new Thread(() -> stoppableReceive(clientPort, clientSock));
            clientThread.start();

            // Start the main server
            mainServer = new MainServer(mainServerNode, mainServerNode);
            Thread.sleep(500);

            // Create and send a packet destined for the client
            PacketInfo packetInfo = new PacketInfo();
            packetInfo.setLength(22);
            packetInfo.setType(NetworkType.SAMECLUSTER.ordinal());
            packetInfo.setConnectionType(NetworkConnectionType.HELLO.ordinal());
            packetInfo.setIpAddress(InetAddress.getByName(clientNode.hostName()));
            packetInfo.setPortNum(clientNode.port());
            packetInfo.setPayload("Same Cluster Routing".getBytes());
            byte[] packet = PacketParser.getPacketParser().createPkt(packetInfo);

            // Send packet to main server
            try (Socket socket = new Socket(mainServerNode.hostName(), mainServerNode.port())) {
                DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
                dataOut.write(packet);
                dataOut.flush();
            }

            clientThread.join(2000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Teardown
            if (mainServer != null) {
                mainServer.close();
            }
            if (clientSock[0] != null && !clientSock[0].isClosed()) {
                try {
                    clientSock[0].close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (clientThread != null && clientThread.isAlive()) {
                clientThread.interrupt();
            }
            System.out.println("Finished same-cluster routing test.");
        }
    }

    /**
     * Tests that the server remains stable after receiving a malformed packet.
     */
    @org.junit.jupiter.api.Test
    public void testMalformedPacket() {
        System.out.println("Testing malformed packet...");
        final int mainServerPort = 8050;
        MainServer mainServer = null;

        try {
            // Setup
            final ClientNode mainServerNode = new ClientNode("127.0.0.1", mainServerPort);
            Topology.getTopology().replaceNetwork(new NetworkStructure(new ArrayList<>(), new ArrayList<>()));
            mainServer = new MainServer(mainServerNode, mainServerNode);
            Thread.sleep(500);

            // Send a malformed packet
            byte[] malformedPacket = new byte[]{1, 2, 3};
            try (Socket socket = new Socket(mainServerNode.hostName(), mainServerNode.port())) {
                DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
                dataOut.write(malformedPacket);
                dataOut.flush();
            }
            System.out.println("Sent a malformed packet.");
            Thread.sleep(500); // Give server time to process and fail

            // Send a valid packet to see if the server is still alive
            System.out.println("Sending a valid HELLO packet to check server status...");
            try (Socket testSocket = new Socket(mainServerNode.hostName(), mainServerNode.port())) {
                DataOutputStream out = new DataOutputStream(testSocket.getOutputStream());
                DataInputStream in = new DataInputStream(testSocket.getInputStream());

                byte[] helloPacket = getHelloPacket(new ClientNode("127.0.0.1", testSocket.getLocalPort()));
                out.write(helloPacket);

                // The server should respond with a NETWORK packet.
                testSocket.setSoTimeout(2000);
                byte[] response = in.readAllBytes();
                if (response.length > 0) {
                    System.out.println("Server responded to valid packet after malformed one. Server is stable.");
                } else {
                    System.out.println("Server did NOT respond after malformed packet.");
                }
            }

        } catch (Exception e) {
            // If this exception happens, it might mean the server died.
            System.out.println("An exception occurred during the test, possibly because the server is down.");
            e.printStackTrace();
        } finally {
            // 4. Teardown
            if (mainServer != null) {
                mainServer.close();
            }
            System.out.println("Finished malformed packet test.");
        }
    }
    /**
     * Test for routing functionality to a client in another cluster.
     */
    @org.junit.jupiter.api.Test
    public void testRoutingToOtherCluster() {
        System.out.println("Testing another routing to other cluster...");
        final int mainServerPort = 8060;
        final int otherServerPort = 8061;
        final int clientPort = 9060;
        final ServerSocket[] otherServerSock = new ServerSocket[1];
        MainServer mainServer = null;
        Thread otherServerThread = null;

        try {
            //Setup
            final ClientNode mainServerNode = new ClientNode("127.0.0.1", mainServerPort);
            final ClientNode otherServerNode = new ClientNode("127.0.0.1", otherServerPort);
            final ClientNode clientNode = new ClientNode("127.0.0.1", clientPort);

            // Reset and manually set up topology
            Topology.getTopology().replaceNetwork(new NetworkStructure(new ArrayList<>(), new ArrayList<>()));
            Topology.getTopology().addClient(mainServerNode);
            Topology.getTopology().addClient(otherServerNode);
            Topology.getTopology().addClient(clientNode);

            // Start a listener for the "other server"
            otherServerThread = new Thread(() -> stoppableReceive(otherServerPort, otherServerSock));
            otherServerThread.start();

            // Start the main server
            mainServer = new MainServer(mainServerNode, mainServerNode);
            Thread.sleep(500); // Give server time to start

            // Create and send a packet destined for a client in another cluster
            PacketInfo packetInfo = new PacketInfo();
            packetInfo.setLength(22);
            packetInfo.setType(NetworkType.OTHERCLUSTER.ordinal());
            packetInfo.setConnectionType(NetworkConnectionType.HELLO.ordinal());
            packetInfo.setIpAddress(InetAddress.getByName(clientNode.hostName()));
            packetInfo.setPortNum(clientNode.port());
            packetInfo.setPayload("Another Routing Test".getBytes());
            byte[] packet = PacketParser.getPacketParser().createPkt(packetInfo);

            // Send packet to main server, which should route it to otherServerNode
            try (Socket socket = new Socket(mainServerNode.hostName(), mainServerNode.port())) {
                DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
                dataOut.write(packet);
                dataOut.flush();
            }

            otherServerThread.join(2000); // Wait for the thread to finish, with a timeout

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Teardown
            if (mainServer != null) {
                mainServer.close();
            }
            if (otherServerSock[0] != null && !otherServerSock[0].isClosed()) {
                try {
                    otherServerSock[0].close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (otherServerThread != null && otherServerThread.isAlive()) {
                otherServerThread.interrupt();
            }
            System.out.println("Finished another routing to other cluster test.");
        }
    }
}