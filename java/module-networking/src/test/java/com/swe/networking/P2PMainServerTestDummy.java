package com.swe.networking;
import java.net.UnknownHostException;

import org.junit.Test;


public class P2PMainServerTestDummy {

    private int mainServerPort = 8000;
    private String loopBackAddress = "10.32.0.41";
    private String loganAddr = "10.32.0.41";

    @Test
    public void testMainServerInitialization() throws UnknownHostException {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%n");
        ClientNode mainServerNode = new ClientNode(loopBackAddress, mainServerPort);
        ClientNode p2pserverNode = new ClientNode("10.32.0.41", mainServerPort + 3);
//        Topology topology = Topology.getTopology();
//        topology.addUser(p2pserverNode, mainServerNode);
        Networking networking = Networking.getNetwork();
        networking.addUser(p2pserverNode, mainServerNode);
        final MessageListener func = (byte[] data) -> {
            System.out.println("This Server Received data: " + data.length);
        };
        networking.subscribe(0, func);
        ClientNode[] dest = {mainServerNode};
        networking.sendData(new byte[5*1024], dest, 0, 0);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        networking.closeNetworking();
//        topology.closeTopology();
    }

}
