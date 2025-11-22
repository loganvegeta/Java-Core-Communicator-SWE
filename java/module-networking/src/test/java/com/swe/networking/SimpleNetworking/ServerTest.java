package com.swe.networking.SimpleNetworking;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.swe.networking.ClientNode;
import com.swe.networking.ModuleType;

public class ServerTest {

    private static final String Client1 = "127.0.0.1";
    private static final String Client2 = "127.0.0.1";
    private static final String ServerIp = "127.0.0.1";
    private static final int Client1Port = 10001;
    private static final int Client2Port = 10002;
    private static final int ServerPort = 10000;
    private static final SimpleNetworking simpleNetworking = SimpleNetworking.getSimpleNetwork();
    private static final PacketParser parser = PacketParser.getPacketParser();

    @org.junit.jupiter.api.Test
    public void testServerIf() {

        try {
            final ClientNode clientNode = new ClientNode(Client1, Client1Port);
            final ClientNode serverNode = new ClientNode(ServerIp, ServerPort);

            final Client sender = new Client(clientNode);
            final Server server = new Server(serverNode);
            final MessageListener func = (byte[] data) -> {
                System.out.println("Received data: " + new String(data, StandardCharsets.UTF_8));
            };
            simpleNetworking.subscribe(ModuleType.CHAT, func);

            final Thread serverThread = new Thread(() -> {
                try {
                    server.receive();
                } catch (IOException e) {
                    System.err.println("Server receive error: " + e.getMessage());
                }
            });
            serverThread.start();
            Thread.sleep(500);

            final ClientNode[] destinations = {serverNode };
            final String msg = "Direct message to server";
            final byte[] testData = msg.getBytes(StandardCharsets.UTF_8);

            sender.send(testData, destinations, serverNode, ModuleType.CHAT);
            serverThread.join(2000);
            System.out.println("Test1 completed");

        } catch (Exception e) {
            System.err.println("Test1 failed: " + e.getMessage());
        }
    }

    @org.junit.jupiter.api.Test
    public void testServerElse() {

        try {
            final ClientNode serverNode = new ClientNode(ServerIp, ServerPort);
            final ClientNode clientNode1 = new ClientNode(Client1, Client1Port);
            final ClientNode clientNode2 = new ClientNode(Client2, Client2Port);

            final Client sender = new Client(clientNode1);
            final Server server = new Server(serverNode);
            final Client receiver = new Client(clientNode2);

            final MessageListener func = (byte[] data) -> {
                System.out.println("Received data: " + new String(data, StandardCharsets.UTF_8));
            };
            simpleNetworking.subscribe(ModuleType.CHAT, func);

            final Thread serverThread = new Thread(() -> {
                try {
                    server.receive();
                } catch (IOException e) {
                    System.err.println("Server receive error: " + e.getMessage());
                }
            });
            final Thread receThread = new Thread(() -> {
                try {
                    receiver.receive();
                } catch (IOException e) {
                    System.err.println("Server receive error: " + e.getMessage());
                }
            });
            serverThread.start();
            receThread.start();
            Thread.sleep(500);

            final ClientNode[] destinations = { clientNode2 };
            final String msg = "Message received via server";
            final byte[] testData = msg.getBytes(StandardCharsets.UTF_8);

            sender.send(testData, destinations, serverNode, ModuleType.CHAT);
            serverThread.join(2000);
            System.out.println("Test2 completed");

        } catch (Exception e) {
            System.err.println("Test2 failed: " + e.getMessage());
        }
    }

}