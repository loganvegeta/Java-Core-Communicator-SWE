package com.swe.networking;

import org.junit.Test;

import com.swe.networking.PriorityQueue.PacketPriority;

public class BroadCastTests {

    private String mainServerAddress = "127.0.0.1";
    private int mainServerPort = 8000;
    
    @Test
    public void testBroadcast() {
        ClientNode node = new ClientNode(mainServerAddress, mainServerPort);
        Networking networking = Networking.getNetwork();
        networking.addUser(node, node);
        new Thread(() -> {
            networking.start();
        }).start();
        try {
            Thread.sleep(20000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testListenForBroadcast0() {

        String ip = "127.0.0.1";
        int port = 8004;
        ClientNode node = new ClientNode(ip, port);
        ClientNode mainServerNode = new ClientNode(mainServerAddress, mainServerPort);
        Networking networking = Networking.getNetwork();
        networking.addUser(node, mainServerNode);
        try {
            Thread.sleep(20000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

}
