package com.swe.networking;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

public class P2PClientTest {

    final ClientNode mainServer = new ClientNode("127.0.0.1", 5000);
    ClientNode deviceNode;
    final ClientNode clusterServerNode = new ClientNode("127.0.0.1", 5002);
    final ClientNode otherClientNode = new ClientNode("127.0.0.1", 5003);
    private List<Thread> mockServerThreads;

    final PacketParser packetParser = PacketParser.getPacketParser();
    final NetworkSerializer serializer = NetworkSerializer.getNetworkSerializer();
    final Topology topology = Topology.getTopology();
//    final TCPCommunicator mockCommunicator = new TCPCommunicator(deviceNode.port());

    final Semaphore semt_t = new Semaphore(10);

    private P2PClient testClient;

    public NetworkStructure getMockNetworkStructure() {
        final List<List<ClientNode>> clusters = new ArrayList<>();
        final List<ClientNode> clusterServers = new ArrayList<>();

        // Cluster 0 (another cluster)
        final List<ClientNode> cluster0 = new ArrayList<>();
        final ClientNode otherServer = new ClientNode("127.0.0.1", 6000);
        cluster0.add(otherServer);
        clusters.add(cluster0);
        clusterServers.add(otherServer);

        // Cluster 1 (The client's cluster)
        final List<ClientNode> cluster1 = new ArrayList<>();
        cluster1.add(clusterServerNode); // The server
//        cluster1.add(deviceNode);         // The client itself
        cluster1.add(otherClientNode);    // Another client
        clusters.add(cluster1);
        clusterServers.add(clusterServerNode);

        return new NetworkStructure(clusters, clusterServers);
    }

    /**
     * Helper method to send a packet to a specific ClientNode. We use this to
     * send packets *to* our testClient.
     */
    public void sendPacket(final byte[] packet, final ClientNode destNode) {
        try {
            final Socket socket = new Socket(destNode.hostName(), destNode.port());
            final OutputStream out = socket.getOutputStream();
            out.write(packet);
            out.flush();

        } catch (Exception e) {
            System.err.println("Test sendPacket failed: " + e.getMessage());
        }
    }

    /**
     * Helper to create a fully formed packet for sending.
     *
     * @return Raw packet bytes
     */
    private byte[] createTestPacket(final int type, final int connType, final byte[] payload) throws UnknownHostException {
        final PacketInfo info = new PacketInfo();
        info.setType(type);
        info.setConnectionType(connType);
        info.setPayload(payload != null ? payload : new byte[0]);

        info.setIpAddress(InetAddress.getByName(mainServer.hostName()));
        info.setPortNum(mainServer.port());
        int l = 22;
        if (payload != null) {
            l += payload.length;
        }
        info.setLength(l);
        return packetParser.createPkt(info);
    }

    @Before
    public void setUp() {

        topology.replaceNetwork(new NetworkStructure(new ArrayList<>(), new ArrayList<>()));
        testClient = null;
    }

    @After
    public void tearDown() throws InterruptedException {

        if (testClient != null) {
            testClient.close();
            testClient = null;
        }

        System.out.println("test finished");
    }

    private Thread startMockServer(final int port, final AtomicReference<byte[]> packetStore) {
        final Runnable serverLogic = () -> {
            System.out.println("Starting Mock Server on port " + port);
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                serverSocket.setSoTimeout(5000);

                try (Socket socket = serverSocket.accept()) {
                    final byte[] buffer = new byte[1024];
                    final int bytesRead = socket.getInputStream().read(buffer);
                    if (bytesRead > 0) {
                        final byte[] packet = new byte[bytesRead];
                        System.arraycopy(buffer, 0, packet, 0, bytesRead);
                        packetStore.set(packet);
                    }
                    semt_t.release();
                } catch (java.net.SocketTimeoutException e) {
                    System.err.println("Mock server on port " + port + " timed out.");
                    semt_t.release();
                }

            } catch (IOException e) {
                semt_t.release();
                // e.printStackTrace();
            }
        };

        final Thread t = new Thread(serverLogic);
        t.start();
        return t;
    }

    @Test
    public void testClientReceivesNetworkPacket() throws Exception {
        System.out.println("Test for recieving any packet ...............");
        deviceNode = new ClientNode("127.0.0.1", 9001);
        final NetworkStructure network = getMockNetworkStructure();
        network.clusters().get(1).add(deviceNode);

        final ProtocolBase communicator = new TCPCommunicator(9001);
        testClient = new P2PClient(deviceNode, mainServer, communicator);
        Thread.sleep(500);

        // Create the network structure and the packet
        final byte[] networkPayload = serializer.serializeNetworkStructure(network);
        final byte[] networkPacket = createTestPacket(NetworkType.USE.ordinal(), NetworkConnectionType.NETWORK.ordinal(), networkPayload);

        sendPacket(networkPacket, deviceNode);

        Thread.sleep(500);

        assertEquals("Client should be in cluster 1", 1, topology.getClusterIndex(deviceNode));
        assertEquals("Client's server should be clusterServerNode", clusterServerNode, topology.getServer(deviceNode));

    }

    @Test
    public void testClientSendToSingleDestPacket() throws Exception {
        System.out.println("Test for sending data to single dest packet ...............");

        final int DEST_PORT = 6000;
        final AtomicReference<byte[]> receivedData = new AtomicReference<>();

        final Thread serverThread = startMockServer(6000, receivedData);
        Thread.sleep(100);

        deviceNode = new ClientNode("127.0.0.1", 9002);

        final ProtocolBase communicator = new TCPCommunicator(9002);

        testClient = new P2PClient(deviceNode, mainServer, communicator);
        final ClientNode destination = new ClientNode("127.0.0.1", DEST_PORT);
        final byte[] testdata = new byte[]{2, 4, 5};

        semt_t.acquire();
        testClient.send(testdata, destination);

        serverThread.join(100);
    }

    @Test
    public void testClientSendToMultipleDestPacket() throws Exception {
        System.out.println("Test for sending data to multiple dest packet ...............");
        final int DEST_PORT = 6000;
        final AtomicReference<byte[]> receivedData = new AtomicReference<>();
        final Thread serverThread = startMockServer(6000, receivedData);
        Thread.sleep(100);

        deviceNode = new ClientNode("127.0.0.1", 9006);

        final ProtocolBase communicator = new TCPCommunicator(9006);
        testClient = new P2PClient(deviceNode, mainServer, communicator);
        final ClientNode destination1 = new ClientNode("127.0.0.1", 8002);
        final ClientNode destination2 = new ClientNode("127.0.0.1", 8003);

        final byte[] testdata = new byte[]{2, 4, 5};

        final ClientNode[] destination = new ClientNode[]{destination1, destination2};
        semt_t.acquire();
        testClient.send(testdata, destination);
        serverThread.join(100);

    }

    @Test
    public void testALivePacketSending() throws Exception {
        System.out.println("Test for sending alive packet ...............");
        final AtomicReference<byte[]> receivedPacket = new AtomicReference<>();
        final Thread serverThread = startMockServer(clusterServerNode.port(), receivedPacket);
        Thread.sleep(100);

        deviceNode = new ClientNode("127.0.0.1", 9003);
        final NetworkStructure network = getMockNetworkStructure();
        network.clusters().get(1).add(deviceNode);
        topology.replaceNetwork(network);
        semt_t.acquire();

        final ProtocolBase communicator = new TCPCommunicator(9003);
        testClient = new P2PClient(deviceNode, mainServer, communicator);
        setPrivateField(testClient, "clusterServerAddress", clusterServerNode);

        Thread.sleep(5000);

        assertNotNull("Mock server did not receive any packet", receivedPacket.get());
        final PacketInfo info = packetParser.parsePacket(receivedPacket.get());
        assertEquals(NetworkType.USE.ordinal(), info.getType());
        assertEquals(NetworkConnectionType.ALIVE.ordinal(), info.getConnectionType());

        serverThread.join(100);
    }

    @Test
    public void testALivePacketSendingtoNull() throws Exception {
        System.out.println("Test for sending alive packet when cluster is none ...............");
        deviceNode = new ClientNode("127.0.0.1", 9013);

        final ProtocolBase communicator = new TCPCommunicator(9013);
        testClient = new P2PClient(deviceNode, mainServer, communicator);
        Thread.sleep(4000);
    }

    @Test
    public void testHandlingAddReceivedPacket() throws Exception {
        System.out.println("Test for receiving ADD packet ...............");

        deviceNode = new ClientNode("127.0.0.1", 9004);
        final NetworkStructure network = getMockNetworkStructure();
        network.clusters().get(1).add(deviceNode);
        topology.replaceNetwork(network);
        final ProtocolBase communicator = new TCPCommunicator(9004);
        testClient = new P2PClient(deviceNode, mainServer, communicator);
        Thread.sleep(1000);

        final ClientNode newNode = new ClientNode("127.0.0.1", 7000);
        final ClientNetworkRecord addRecord = new ClientNetworkRecord(newNode, 1); // Add to cluster 1
        final byte[] addPayload = serializer.serializeClientNetworkRecord(addRecord);
        final byte[] addPacket = createTestPacket(NetworkType.USE.ordinal(), NetworkConnectionType.ADD.ordinal(),
                addPayload);
        sendPacket(addPacket, deviceNode);
        Thread.sleep(500);

        assertEquals("new node should be in cluster 1", 1, topology.getClusterIndex(newNode));
    }

    @Test
    public void testRecieveModulePacket() throws Exception {
        System.out.println("Test for receiving Module packet ...............");

        deviceNode = new ClientNode("127.0.0.1", 9014);
        final NetworkStructure network = getMockNetworkStructure();
        network.clusters().get(1).add(deviceNode);
        topology.replaceNetwork(network);
        final ProtocolBase communicator = new TCPCommunicator(9014);
        testClient = new P2PClient(deviceNode, mainServer, communicator);
        Thread.sleep(1000);

        final ClientNode newNode = new ClientNode("127.0.0.1", 7000);
        final ClientNetworkRecord addRecord = new ClientNetworkRecord(newNode, 1); // Add to cluster 1
        final byte[] addPayload = serializer.serializeClientNetworkRecord(addRecord);
        final byte[] addPacket = createTestPacket(NetworkType.USE.ordinal(), NetworkConnectionType.MODULE.ordinal(), addPayload);
        sendPacket(addPacket, deviceNode);
        Thread.sleep(500);

    }

    @Test
    public void testHandlingRemoveReceivedPacket() throws Exception {
        System.out.println("Test for receiving REMOVE packet ...............");

        deviceNode = new ClientNode("127.0.0.1", 9005);
        final NetworkStructure network = getMockNetworkStructure();
        network.clusters().get(1).add(deviceNode);
        final byte[] networkPacket = createTestPacket(NetworkType.USE.ordinal(), NetworkConnectionType.NETWORK.ordinal(), serializer.serializeNetworkStructure(network));

        final ProtocolBase communicator = new TCPCommunicator(9005);
        testClient = new P2PClient(deviceNode, mainServer, communicator);

        sendPacket(networkPacket, deviceNode);
        Thread.sleep(1000);

        assertEquals("otherClientNode should be in cluster 1", 1, topology.getClusterIndex(otherClientNode));

        final ClientNetworkRecord removeRecord = new ClientNetworkRecord(otherClientNode, 1);
        final byte[] removePayload = serializer.serializeClientNetworkRecord(removeRecord);
        final byte[] removePacket = createTestPacket(NetworkType.USE.ordinal(), NetworkConnectionType.REMOVE.ordinal(), removePayload);

        sendPacket(removePacket, deviceNode);
        Thread.sleep(1000);

        assertEquals("otherClientNode should be removed", -1, topology.getClusterIndex(otherClientNode));
    }

    @Test
    public void testHandlingNetworkReceivedPacket() throws Exception {
        System.out.println("Test for receiving NETWORK packet ...............");

        deviceNode = new ClientNode("127.0.0.1", 9007);
        final NetworkStructure network = getMockNetworkStructure();
        network.clusters().get(1).add(deviceNode);
        final byte[] networkPacket = createTestPacket(NetworkType.USE.ordinal(), NetworkConnectionType.NETWORK.ordinal(), serializer.serializeNetworkStructure(network));

        final ProtocolBase communicator = new TCPCommunicator(9007);
        testClient = new P2PClient(deviceNode, mainServer, communicator);

        sendPacket(networkPacket, deviceNode);
        Thread.sleep(100);
    }

    @Test
    public void testHandlingBroadcastPacket() throws Exception {
        System.out.println("Test for receiving Broadcast packet ...............");

        deviceNode = new ClientNode("127.0.0.1", 9015);
        final NetworkStructure network = getMockNetworkStructure();
        network.clusters().get(1).add(deviceNode);
        topology.replaceNetwork(network);
        final ProtocolBase communicator = new TCPCommunicator(9015);
        testClient = new P2PClient(deviceNode, mainServer, communicator);
        Thread.sleep(1000);

        final byte[] addPacket = createTestPacket(NetworkType.USE.ordinal(), NetworkConnectionType.MODULE.ordinal(), null);
        final PacketInfo info = packetParser.parsePacket(addPacket);
        info.setBroadcast(1);
        final byte[] updatedPacket = packetParser.createPkt(info);
        sendPacket(updatedPacket, deviceNode);
        Thread.sleep(500);
    }

    @Test
    public void testHandlingHello_Alive_Unknown_Packet() throws Exception {
        System.out.println("Test for receiving hello or alive packet ...............");

        deviceNode = new ClientNode("127.0.0.1", 9008);
        final NetworkStructure network = getMockNetworkStructure();
        network.clusters().get(1).add(deviceNode);
        final byte[] networkPacket = createTestPacket(NetworkType.USE.ordinal(), NetworkConnectionType.NETWORK.ordinal(), serializer.serializeNetworkStructure(network));

        final ProtocolBase communicator = new TCPCommunicator(9008);
        testClient = new P2PClient(deviceNode, mainServer, communicator);

        sendPacket(networkPacket, deviceNode);
        Thread.sleep(100);

        final byte[] alivePacket = createTestPacket(NetworkType.USE.ordinal(), NetworkConnectionType.ALIVE.ordinal(), null);
        final byte[] helloPacket = createTestPacket(NetworkType.USE.ordinal(), NetworkConnectionType.HELLO.ordinal(), null);
        sendPacket(helloPacket, deviceNode);
        sendPacket(alivePacket, deviceNode);
        Thread.sleep(500);
    }

    @Test
    public void testHandlingClosePacket() throws Exception {
        System.out.println("Test for receiving close packet ...............");

        deviceNode = new ClientNode("127.0.0.1", 9009);
        final ProtocolBase communicator = new TCPCommunicator(9009);
        testClient = new P2PClient(deviceNode, mainServer, communicator);

        try {
            final byte[] helloPacket = createTestPacket(NetworkType.USE.ordinal(), NetworkConnectionType.HELLO.ordinal(), null);
            sendPacket(helloPacket, deviceNode);
        } catch (IOException e) {
            fail("Client socket was not open before test: " + e.getMessage());
        }

        final byte[] closePacket = createTestPacket(NetworkType.USE.ordinal(), NetworkConnectionType.CLOSE.ordinal(),
                null);
        sendPacket(closePacket, deviceNode);

        // checking
        Thread.sleep(500);
        boolean conn = true;
        try {
            final byte[] helloPacket = createTestPacket(NetworkType.USE.ordinal(),
                    NetworkConnectionType.HELLO.ordinal(),
                    null);
            sendPacket(helloPacket, deviceNode);
        } catch (IOException e) {
            conn = false;
        }

        assertTrue(conn);
    }

    @Test
    public void testDroppedTypePacket() throws Exception {
        System.out.println("Test for receiving dropped packet ...............");

        deviceNode = new ClientNode("127.0.0.1", 9010);
        final NetworkStructure network = getMockNetworkStructure();
        network.clusters().get(1).add(deviceNode);
        final byte[] networkPacket = createTestPacket(NetworkType.USE.ordinal(), NetworkConnectionType.NETWORK.ordinal(), serializer.serializeNetworkStructure(network));

        final ProtocolBase communicator = new TCPCommunicator(9010);
        testClient = new P2PClient(deviceNode, mainServer, communicator);

        sendPacket(networkPacket, deviceNode);
        Thread.sleep(100);

        final byte[] pkt1 = createTestPacket(NetworkType.CLUSTERSERVER.ordinal(), NetworkConnectionType.HELLO.ordinal(), null);
        final byte[] pkt2 = createTestPacket(NetworkType.SAMECLUSTER.ordinal(), NetworkConnectionType.HELLO.ordinal(), null);
        final byte[] pkt3 = createTestPacket(NetworkType.OTHERCLUSTER.ordinal(), NetworkConnectionType.HELLO.ordinal(), null);
        final byte[] pkt4 = createTestPacket(4, NetworkConnectionType.HELLO.ordinal(), null);
        sendPacket(pkt1, deviceNode);
        sendPacket(pkt2, deviceNode);
        sendPacket(pkt3, deviceNode);
        sendPacket(pkt4, deviceNode);
        Thread.sleep(500);
    }

    @Test
    public void testUpdateClusterServerNull() throws Exception {
        System.out.println("Test for updating cluster server is it null ...............");
        deviceNode = new ClientNode("127.0.0.1", 9011);
        final ProtocolBase communicator = new TCPCommunicator(9011);
        testClient = new P2PClient(deviceNode, mainServer, communicator);
        testClient.updateClusterServer();
    }

    @Test
    public void testclosewhenEverythingNull() throws Exception {
        System.out.println("Test for close when all thread already terminated  ...............");

        deviceNode = new ClientNode("127.0.0.1", 9012);
        final ProtocolBase communicator = new TCPCommunicator(9012);
        testClient = new P2PClient(deviceNode, mainServer, communicator);

        testClient.close();
        Thread.sleep(100);

        setPrivateField(testClient, "aliveScheduler", null);
        setPrivateField(testClient, "receiveThread", null);
        setPrivateField(testClient, "communicator", null);
        setPrivateField(testClient, "running", true);

        testClient.close();

        assertTrue(true);
    }

    private static void setPrivateField(final Object target, final String fieldName, final Object value)
            throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

}
