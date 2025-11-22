package com.swe.networking;

import java.net.UnknownHostException;

import org.junit.Test;

public class P2PMainServerTest {

    private int mainServerPort = 8000;
//    byte[] payload =
    private String loopBackAddress = "10.128.4.223";

    @Test
    public void testMainServerInitialization() throws UnknownHostException {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%n");
        ClientNode mainServerNode = new ClientNode(loopBackAddress, mainServerPort);
        Networking networking = Networking.getNetwork();
        // Topology topology = Topology.getTopology();
        // topology.addUser(mainServerNode, mainServerNode);
        networking.addUser(mainServerNode, mainServerNode);
        final MessageListener func = (byte[] data) -> {
            System.out.println("This Server Received data: " + data.length);
        };
        networking.subscribe(0, func);
        try {
            Thread.sleep(500000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        networking.closeNetworking();
    }

}
