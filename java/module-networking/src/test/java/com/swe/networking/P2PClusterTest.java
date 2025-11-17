package com.swe.networking;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class P2PClusterTest {
 
    private int mainServerPort = 9000;
    private String loopBackAddress = "127.0.0.1";

    @Test
    public void testClusterInitialization() throws UnknownHostException {
        ClientNode serverNode = new ClientNode(loopBackAddress, mainServerPort);
        ClientNode p2pserverNode = new ClientNode(loopBackAddress, mainServerPort + 1);
        P2PCluster cluster0 = new P2PCluster();
        cluster0.addUser(p2pserverNode, serverNode);
    }

}