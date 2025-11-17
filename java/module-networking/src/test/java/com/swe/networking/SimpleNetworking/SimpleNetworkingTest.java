package com.swe.networking.SimpleNetworking;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import com.swe.networking.ClientNode;
import com.swe.networking.ModuleType;
import com.swe.networking.PacketInfo;
import com.swe.networking.PacketParser;

/**
 * Test class for simplenetworking class.
 */
public class SimpleNetworkingTest {

    /**
     * Function to test the simpleNetwokring server receive.
     */
    @org.junit.jupiter.api.Test
    public void simpleNetworkingServerTestReceive() {
        try {
            final Integer sleepTime = 2000;
            final String localAddress = "10.32.0.41";
            final SimpleNetworking network = SimpleNetworking.getSimpleNetwork();
            final ClientNode device = new ClientNode(localAddress, 8000);
            final ClientNode mainServer = new ClientNode(localAddress, 8000);
            network.addUser(device, mainServer);
            final MessageListener func = (byte[] data) -> {
                System.out.println("Server Received data: " + data.length);
            };
            network.subscribe(ModuleType.CHAT, func);
            sendServer();
            Thread.sleep(sleepTime);
            network.closeNetworking();
        } catch (InterruptedException ex) {
        }
    }

    /**
     * Test function to send message.
     *
     * @throws InterruptedException when sleep is interrupted
     */
    public void sendServer() throws InterruptedException {
        final Socket destSocket = new Socket();
        try {
            final Integer port = 8000;
            final Integer timeout = 5000;
            final Integer sleepTime = 2000;
            destSocket.connect(new InetSocketAddress("127.0.0.1", port), timeout);
            final DataOutputStream dataOut = new DataOutputStream(destSocket.getOutputStream());
            final String data = "Hello World";
            System.out.println("Sent data of size " + data.length());
            final ClientNode dest = new ClientNode("10.32.0.41", 8000);
            final byte[] packet = createTestPacket(data.getBytes(), dest, ModuleType.CHAT.ordinal());
            dataOut.write(packet);
            sendServer1(destSocket.getLocalPort());
            Thread.sleep(sleepTime);
            destSocket.close();
        } catch (IOException ex) {
        }
    }

    /**
     * Test function to send message.
     *
     * @param destPort the destination to send to
     */
    public void sendServer1(final int destPort) {
        final Socket destSocket = new Socket();
        try {
            final Integer port = 8000;
            final Integer timeout = 5000;
            destSocket.connect(new InetSocketAddress("127.0.0.1", port), timeout);
            final DataOutputStream dataOut = new DataOutputStream(destSocket.getOutputStream());
            final String data = "Hello World";
            System.out.println("Sent data of size " + data.length());
            final ClientNode dest = new ClientNode("10.32.0.41", destPort);
            final byte[] packet = createTestPacket(data.getBytes(), dest, ModuleType.CHAT.ordinal());
            dataOut.write(packet);
            destSocket.close();
        } catch (IOException ex) {
        }
    }

    /**
     * Function to test the simpleNetwokring client receive.
     */
    @org.junit.jupiter.api.Test
    public void simpleNetworkingClientTestReceive() {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%5%n");
        try {
            final Integer sleepTime = 2000;
            final SimpleNetworking network = SimpleNetworking.getSimpleNetwork();
            final ClientNode device = new ClientNode("127.0.0.1", 9002);
            final ClientNode mainServer = new ClientNode("127.0.0.1", 9003);
            final Server server = new Server(mainServer);
            final Thread serverThread = new Thread(() -> {
                try {
                    while (true) {
                        server.receive();
                    }
                } catch (IOException e) {
                    System.err.println("Server receive error: " + e.getMessage());
                }
            });
            serverThread.start();
            network.addUser(device, mainServer);
            final MessageListener func = (byte[] data) -> {
                System.out.println("Received data: " + new String(data, StandardCharsets.UTF_8));
            };
            network.subscribe(ModuleType.CHAT, func);
            sendClient();
            Thread.sleep(sleepTime);
            network.closeNetworking();
            // serverThread.interrupt();
        } catch (InterruptedException ex) {
        }
    }

    /**
     * Test function to send message.
     */
    public void sendClient() {
        final Socket destSocket = new Socket();
        try {
            final Integer port = 9003;
            final Integer timeout = 5000;
            destSocket.connect(new InetSocketAddress("127.0.0.1", port), timeout);
            final DataOutputStream dataOut = new DataOutputStream(destSocket.getOutputStream());
            final String data = "Hello World !!!";
            final ClientNode dest = new ClientNode("10.32.0.41", 9002);
            final byte[] packet = createTestPacket(data.getBytes(), dest, ModuleType.CHAT.ordinal());
            dataOut.write(packet);
            destSocket.close();
        } catch (IOException ex) {
        }
    }

    /**
     * Function to test subscribe in simplenetworking.
     */
    @org.junit.jupiter.api.Test
    public void simpleNetworkingSubscribeTest() {
        final SimpleNetworking network = SimpleNetworking.getSimpleNetwork();
        final MessageListener func = (byte[] data) -> {
            System.out.println("Received data: " + new String(data, StandardCharsets.UTF_8));
        };
        network.subscribe(ModuleType.CHAT, func);
        network.subscribe(ModuleType.CHAT, func);
        network.removeSubscription(ModuleType.CHAT);
        network.removeSubscription(ModuleType.CHAT);
    }

    /**
     * Function to test simpleNetworking server send.
     */
    @org.junit.jupiter.api.Test
    public void simpleNetworkingServerSendTest() throws IOException {
        try {
            final ClientNode cdevice = new ClientNode("10.32.0.41", 8005);
            final int messageSize = 1 * 1024 * 1024;
            final byte[] message = new byte[messageSize];
            final Client client = new Client(cdevice);
            final Thread clientThread = new Thread(() -> {
                try {
                    while (true) {
                        client.receive();
                    }
                } catch (IOException e) {
                    System.err.println("Server receive error: " + e.getMessage());
                }
            });
            clientThread.start();
            final SimpleNetworking network = SimpleNetworking.getSimpleNetwork();
            final ClientNode device = new ClientNode("10.32.0.41", 8100);
            final ClientNode mainServer = new ClientNode("10.32.0.41", 8100);
            network.addUser(device, mainServer);
            final MessageListener func = (byte[] data) -> {
                final int sizeMB = 1024 * 1024;
                System.out.println("Received data length : " + data.length / sizeMB + " MB");
            };
            network.subscribe(ModuleType.CHAT, func);
            final String data = "Hello world to the new world";
            System.out.println("Sending Data length " + data.length());
            final ClientNode dest = new ClientNode("127.0.0.1", 8005);
            final ClientNode[] dests = {dest, dest};
            network.sendData(message, dests, ModuleType.CHAT, 0);
            final int sleepSeconds = 2000;
            Thread.sleep(sleepSeconds);
            clientThread.interrupt();
            network.closeNetworking();
            client.closeUser();
        } catch (InterruptedException ex) {
        }
    }

    /**
     * Test to check socket exceptions.
     */
    @org.junit.jupiter.api.Test
    public void simpleNetworkingIOTest() {
        final ClientNode device = new ClientNode("127.0.0.1", 8200);
        final Server server = new Server(device);
        final String data = "Hello from server !!!";
        final ClientNode dest = new ClientNode("127.0.0.1", 8201);
        final ClientNode[] dests = {dest};
        server.send(data.getBytes(), dests, device, ModuleType.CHAT);
        server.sendPkt(data.getBytes(), dests, device);
        server.closeUser();
        // network.closeNetworking();
    }

    /**
     * Function to create a test packet.
     *
     * @param data the data of payload
     * @param dest the dest to send
     * @param module the module to send to
     * @return the new packet
     * @throws UnknownHostException if the dest is not present
     */
    public byte[] createTestPacket(final byte[] data, final ClientNode dest, final int module)
            throws UnknownHostException {
        final int headerSize = PacketParser.getHeaderSize();
        final PacketInfo pkt = new PacketInfo();
        pkt.setLength(headerSize + data.length);
        pkt.setBroadcast(0);
        pkt.setType(0);
        pkt.setConnectionType(0);
        pkt.setPayload(data);
        pkt.setIpAddress(InetAddress.getByName(dest.hostName()));
        pkt.setPortNum(dest.port());
        pkt.setChunkNum(0);
        pkt.setChunkLength(1);
        pkt.setMessageId(0);
        pkt.setModule(module);
        final byte[] packet = PacketParser.getPacketParser().createPkt(pkt);
        return packet;
    }

    /**
     * Function to test the error in subscirbing.
     */
    @org.junit.jupiter.api.Test
    public void testErrorSubscribe() {
        final SimpleNetworking network = SimpleNetworking.getSimpleNetwork();
        final String localAddress = "10.32.0.41";
        final ClientNode device = new ClientNode(localAddress, 8103);
        final ClientNode mainServer = new ClientNode(localAddress, 8103);
        network.addUser(device, mainServer);
        final MessageListener func = (byte[] data) -> {
            System.out.println("Server Received data: " + data.length);
        };
        network.subscribe(ModuleType.CHAT, func);
        network.subscribe(ModuleType.CHAT, func);
        network.closeNetworking();
    }
}
