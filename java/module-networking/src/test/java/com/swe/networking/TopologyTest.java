package com.swe.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test class for the topology class.
 */
class TopologyTest {

    /**
     * Function to test the receive by the main server.
     */
    @org.junit.jupiter.api.Test
    void mainServerReceiveTestHello() {
        try {
            final int sleepTime = 15000;
            final Integer port = 8000;
            final Integer timeout = 5000;
            final ClientNode server = new ClientNode("127.0.0.1", 8000);
            final Topology topology = Topology.getTopology();
            topology.addUser(server, server);
            final Socket destSocket = new Socket();
            destSocket.connect(new InetSocketAddress("127.0.0.1", port), timeout);
            final Socket destSocket1 = new Socket();
            // destSocket1.connect(new InetSocketAddress("127.0.0.1", port), timeout);
            // sendHello(destSocket);
            // sendHello(destSocket1);
            // sendRemove(destSocket);
            Thread.sleep(sleepTime);
            // topology.closeTopology();
        } catch (InterruptedException ex) {
            System.out.println("Error 1...");
        } catch (IOException e) {
            System.out.println("Error 2...");
        }
    }

    /**
     * Test function to send data.
     */
    public void sendHello(final Socket destSocket) {
        try {

            final int sleepTime = 1000;
            System.out.println("Client Socket " + destSocket.getLocalAddress().toString() + destSocket.getLocalPort());
            final Thread receiveThread = new Thread(() -> receiveHello(destSocket));
            receiveThread.start();
            Thread.sleep(sleepTime);
            final DataOutputStream dataOut = new DataOutputStream(destSocket.getOutputStream());
            final ClientNode client = new ClientNode("127.0.0.1", destSocket.getLocalPort());
            final byte[] packet = getHelloPacket(client);
            dataOut.write(packet);
            dataOut.flush();
            // receiveThread.interrupt();
            // destSocket.close();
        } catch (IOException | InterruptedException ex) {
        }
    }

    public void sendRemove(final Socket destSocket) {
        try {

            destSocket.setTcpNoDelay(true);
            final int sleepTime = 1000;
            final Thread receiveThread = new Thread(() -> receiveRemove(destSocket));
            receiveThread.start();
            Thread.sleep(sleepTime);
            final DataOutputStream dataOut = new DataOutputStream(destSocket.getOutputStream());
            final ClientNode client = new ClientNode("127.0.0.1", destSocket.getLocalPort());
            final byte[] packet = getRemovePacket(client);
            dataOut.write(packet);
            dataOut.flush();
            // receiveThread.interrupt();
            // destSocket.close();
        } catch (IOException | InterruptedException ex) {
        }
    }

    public void receiveHello(final Socket socket) {
        try {
            final DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            final int byteBufferSize = 2048;
            final byte[] buffer = new byte[byteBufferSize];
            while ((dataIn.read(buffer)) != -1) {
                final PacketInfo pkt = PacketParser.getPacketParser().parsePacket(buffer);
                final NetworkStructure network = NetworkSerializer.getNetworkSerializer()
                        .deserializeNetworkStructure(pkt.getPayload());
                System.out.println("Hello Response: " + network);
            }

        } catch (IOException ex) {
        }
    }

    public void receiveRemove(final Socket socket) {
        try {
            final DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            final int byteBufferSize = 2048;
            final byte[] buffer = new byte[byteBufferSize];
            while ((dataIn.read(buffer)) != -1) {
                final PacketInfo pkt = PacketParser.getPacketParser().parsePacket(buffer);
                System.out.println("Remove Response: " + new String(pkt.getPayload()));
            }

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
            final int idx = topology.getClusterIndex(client);
            final ClientNetworkRecord record = new ClientNetworkRecord(client, idx);
            final byte[] clientBytes = NetworkSerializer.getNetworkSerializer().serializeClientNetworkRecord(record);
            pkt.setLength(packetHeaderSize + clientBytes.length);
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
            pkt.setPayload(clientBytes);
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

    public byte[] getAlivePacket(final ClientNode client) {
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
            pkt.setConnectionType(NetworkConnectionType.ALIVE.ordinal());
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

    public byte[] getClosePacket(final ClientNode client) {
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
            pkt.setConnectionType(NetworkConnectionType.CLOSE.ordinal());
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

    /**
     * Function to test alive the receive by the main server.
     */
    @org.junit.jupiter.api.Test
    void mainServerReceiveTestAlive() {
        try {
            final int sleepTime = 1000;
            final Integer port = 8000;
            final Integer timeout = 5000;
            final ClientNode server = new ClientNode("127.0.0.1", 8000);
            final Topology topology = Topology.getTopology();
            topology.addUser(server, server);
            final Socket destSocket = new Socket();
            destSocket.connect(new InetSocketAddress("127.0.0.1", port), timeout);
            sendHello(destSocket);
            sendAlive(destSocket);
            Thread.sleep(sleepTime);
            topology.closeTopology();
        } catch (InterruptedException ex) {
            System.out.println("Error 1...");
        } catch (IOException e) {
            System.out.println("Error 2...");
        }
    }

    /**
     * Test function to send data.
     */
    public void sendAlive(final Socket destSocket) {
        try {

            destSocket.setTcpNoDelay(true);
            final int sleepTime = 1000;
            final Thread receiveThread = new Thread(() -> receiveAlive(destSocket));
            receiveThread.start();
            Thread.sleep(sleepTime);
            final DataOutputStream dataOut = new DataOutputStream(destSocket.getOutputStream());
            final ClientNode client = new ClientNode("127.0.0.1", destSocket.getLocalPort());
            final byte[] packet = getAlivePacket(client);
            dataOut.write(packet);
            dataOut.flush();
            // receiveThread.interrupt();
            // destSocket.close();
        } catch (IOException | InterruptedException ex) {
        }
    }

    public void receiveAlive(final Socket socket) {
        try {
            final DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            final int byteBufferSize = 2048;
            final byte[] buffer = new byte[byteBufferSize];
            while ((dataIn.read(buffer)) != -1) {
                final PacketInfo pkt = PacketParser.getPacketParser().parsePacket(buffer);
                System.out.println("Alive Response: " + new String(pkt.getPayload()));
            }

        } catch (IOException ex) {
        }
    }

    /**
     * Function to test alive the receive by the main server.
     */
    @org.junit.jupiter.api.Test
    void mainServerReceiveTestOtherCluster() {
        try {
            final int sleepTime = 1000;
            final Integer port = 8000;
            final Integer timeout = 5000;
            final ClientNode server = new ClientNode("127.0.0.1", 8000);
            final Topology topology = Topology.getTopology();
            topology.addUser(server, server);
            final Socket destSocket = new Socket();
            destSocket.connect(new InetSocketAddress("127.0.0.1", port), timeout);
            sendHello(destSocket);
            sendOtherCluster(destSocket);
            Thread.sleep(sleepTime);
            topology.closeTopology();
        } catch (InterruptedException ex) {
            System.out.println("Error 1...");
        } catch (IOException e) {
            System.out.println("Error 2...");
        }
    }

    /**
     * Test function to send data.
     */
    public void sendOtherCluster(final Socket destSocket) {
        try {

            destSocket.setTcpNoDelay(true);
            final int sleepTime = 1000;
            final Thread receiveThread = new Thread(() -> receiveOtherCluster(destSocket));
            receiveThread.start();
            Thread.sleep(sleepTime);
            final DataOutputStream dataOut = new DataOutputStream(destSocket.getOutputStream());
            final ClientNode client = new ClientNode("127.0.0.1", destSocket.getLocalPort());
            final byte[] packet = getOtherClusterPacket(client);
            dataOut.write(packet);
            dataOut.flush();
            // receiveThread.interrupt();
            // destSocket.close();
        } catch (IOException | InterruptedException ex) {
        }
    }

    public void receiveOtherCluster(final Socket socket) {
        try {
            final DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            final int byteBufferSize = 2048;
            final byte[] buffer = new byte[byteBufferSize];
            while ((dataIn.read(buffer)) != -1) {
                final PacketInfo pkt = PacketParser.getPacketParser().parsePacket(buffer);
                System.out.println("OtherCluster Response: " + new String(pkt.getPayload()));
            }

        } catch (IOException ex) {
        }
    }

    public byte[] getOtherClusterPacket(final ClientNode client) {
        try {
            final int randomFactor = (int) Math.pow(10, 6);
            final PacketParser parser = PacketParser.getPacketParser();
            final PacketInfo pkt = new PacketInfo();
            final int packetHeaderSize = 22;
            pkt.setLength(packetHeaderSize);
            final String data = "Hello Sekai !!!";
            pkt.setType(NetworkType.OTHERCLUSTER.ordinal() + data.length());
            pkt.setPriority(0);
            pkt.setModule(0);
            pkt.setConnectionType(NetworkConnectionType.CLOSE.ordinal());
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

    /**
     * Function to test alive the receive by the main server.
     */
    @org.junit.jupiter.api.Test
    void mainServerReceiveTestSameCluster() {
        try {
            final int sleepTime = 1000;
            final Integer port = 8000;
            final Integer timeout = 5000;
            final ClientNode server = new ClientNode("127.0.0.1", 8000);
            final Topology topology = Topology.getTopology();
            topology.addUser(server, server);
            final Socket destSocket = new Socket();
            destSocket.connect(new InetSocketAddress("127.0.0.1", port), timeout);
            sendHello(destSocket);
            sendSameCluster(destSocket);
            Thread.sleep(sleepTime);
            topology.closeTopology();
        } catch (InterruptedException ex) {
            System.out.println("Error 1...");
        } catch (IOException e) {
            System.out.println("Error 2...");
        }
    }

    /**
     * Test function to send data.
     */
    public void sendSameCluster(final Socket destSocket) {
        try {

            destSocket.setTcpNoDelay(true);
            final int sleepTime = 1000;
            final Thread receiveThread = new Thread(() -> receiveSameCluster(destSocket));
            receiveThread.start();
            Thread.sleep(sleepTime);
            final DataOutputStream dataOut = new DataOutputStream(destSocket.getOutputStream());
            final ClientNode client = new ClientNode("127.0.0.1", destSocket.getLocalPort());
            final byte[] packet = getSameClusterPacket(client);
            dataOut.write(packet);
            dataOut.flush();
            // receiveThread.interrupt();
            // destSocket.close();
        } catch (IOException | InterruptedException ex) {
        }
    }

    public void receiveSameCluster(final Socket socket) {
        try {
            final DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            final int byteBufferSize = 2048;
            final byte[] buffer = new byte[byteBufferSize];
            while ((dataIn.read(buffer)) != -1) {
                final PacketInfo pkt = PacketParser.getPacketParser().parsePacket(buffer);
                System.out.println("SameCluster Response: " + new String(pkt.getPayload()));
            }

        } catch (IOException ex) {
        }
    }

    public byte[] getSameClusterPacket(final ClientNode client) {
        try {
            final int randomFactor = (int) Math.pow(10, 6);
            final PacketParser parser = PacketParser.getPacketParser();
            final PacketInfo pkt = new PacketInfo();
            final int packetHeaderSize = 22;
            final String data = "Hello Sekai !!!";
            pkt.setLength(packetHeaderSize + data.length());
            pkt.setType(NetworkType.SAMECLUSTER.ordinal());
            pkt.setPriority(0);
            pkt.setModule(0);
            pkt.setConnectionType(NetworkConnectionType.CLOSE.ordinal());
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
    void mainServerReceiveTestClose() {
        try {
            final int sleepTime = 1000;
            final Integer port = 8000;
            final Integer timeout = 5000;
            final ClientNode server = new ClientNode("127.0.0.1", 8000);
            final Topology topology = Topology.getTopology();
            topology.addUser(server, server);
            final Socket destSocket = new Socket();
            destSocket.connect(new InetSocketAddress("127.0.0.1", port), timeout);
            sendHello(destSocket);
            sendClose(destSocket);
            Thread.sleep(sleepTime);
            topology.closeTopology();
        } catch (InterruptedException ex) {
            System.out.println("Error 1...");
        } catch (IOException e) {
            System.out.println("Error 2...");
        }
    }

    /**
     * Test function to send data.
     */
    public void sendClose(final Socket destSocket) {
        try {

            destSocket.setTcpNoDelay(true);
            final int sleepTime = 1000;
            final Thread receiveThread = new Thread(() -> receiveClose(destSocket));
            receiveThread.start();
            Thread.sleep(sleepTime);
            final DataOutputStream dataOut = new DataOutputStream(destSocket.getOutputStream());
            final ClientNode client = new ClientNode("127.0.0.1", destSocket.getLocalPort());
            final byte[] packet = getClosePacket(client);
            dataOut.write(packet);
            dataOut.flush();
            // receiveThread.interrupt();
            // destSocket.close();
        } catch (IOException | InterruptedException ex) {
        }
    }

    public void receiveClose(final Socket socket) {
        try {
            final DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            final int byteBufferSize = 2048;
            final byte[] buffer = new byte[byteBufferSize];
            while ((dataIn.read(buffer)) != -1) {
                final PacketInfo pkt = PacketParser.getPacketParser().parsePacket(buffer);
                System.out.println("Close Response: " + new String(pkt.getPayload()));
            }

        } catch (IOException ex) {
        }
    }

    @org.junit.jupiter.api.Test
    public void testGetServer() {
        final Topology topology = Topology.getTopology();
        final ClientNode server = new ClientNode("127.0.0.1", 8000);
        final ClientNode dest1 = new ClientNode("127.0.0.1", 8001);
        topology.addUser(server, server);
        topology.addClient(dest1);
        final List<ClientNode> cluster1 = new ArrayList<>();
        final List<ClientNode> cluster2 = new ArrayList<>();
        cluster1.add(server);
        cluster2.add(dest1);
        final List<ClientNode> servers = new ArrayList<>();
        servers.add(server);
        servers.add(dest1);
        final List<List<ClientNode>> clusters = new ArrayList<>();
        clusters.add(cluster1);
        clusters.add(cluster2);
        final NetworkStructure structure = new NetworkStructure(clusters, servers);
        topology.replaceNetwork(structure);
        final List<ClientNode> clients = topology.getAllClients();
        final ClientNode getserver = topology.getServer(server);
        assertEquals(server, getserver);
        final List<ClientNode> expectedclients = new ArrayList<>();
        expectedclients.add(server);
        expectedclients.add(dest1);
        assertEquals(clients, expectedclients);
    }

    @org.junit.jupiter.api.Test
    void testAddClient() {
        final Topology topology = Topology.getTopology();
        
        // Reset the topology
        final java.util.List<java.util.List<ClientNode>> clusters = new java.util.ArrayList<>();
        final java.util.List<ClientNode> servers = new java.util.ArrayList<>();
        final NetworkStructure emptyNetwork = new NetworkStructure(clusters, servers);
        topology.replaceNetwork(emptyNetwork);

        final ClientNode server = new ClientNode("127.0.0.1", 9000); // Using a different port to avoid conflict
        topology.addUser(server, server);

        // Add rest 5 clients, they should all be in the same cluster
        for (int i = 0; i < 5; i++) {
            topology.addClient(new ClientNode("127.0.0.1", 9001 + i));
        }

        NetworkStructure network = topology.getNetwork();
        assertEquals(1, network.clusters().size());
        assertEquals(6, network.clusters().get(0).size()); // 1 server + 5 clients

        // Add one more client, a new cluster should be created
        topology.addClient(new ClientNode("127.0.0.1", 9006));

        network = topology.getNetwork();
        assertEquals(2, network.clusters().size());
        assertEquals(6, network.clusters().get(0).size());
        assertEquals(1, network.clusters().get(1).size());

        topology.closeTopology();
    }
}