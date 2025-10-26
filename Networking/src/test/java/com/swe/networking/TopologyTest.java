package com.swe.networking;

import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class TopologyTest {

    @org.junit.jupiter.api.Test
    void getServer() {
        Topology topology = new Topology();
        String destination = "localhost";
        ClientNode client = topology.GetServer(destination);
        assertNull(client, "The output is not what is expected");
    }

    @org.junit.jupiter.api.Test
    void addingFirstUser() throws NoSuchFieldException, IllegalAccessException {
        Topology topology = new Topology();
        topology.addUser("10.0.0.1", 4562);
        assertEquals(1, (int) getPrivateField(topology, "numClients"));
        assertEquals(1, (int) getPrivateField(topology, "numClusters"));

        ArrayList<Cluster> clusters = getPrivateField(topology, "clusters");
        assertEquals(1, clusters.size(), "there should be only one cluster");
        Cluster firstCluster = clusters.get(0);
        assertNotNull(firstCluster, "Cluster should not be null");

        //checking for clientIP field
        HashMap<Cluster, ArrayList<ClientNode>> clientsIP = getPrivateField(topology, "clientIP");

        assertTrue(clientsIP.containsKey(firstCluster), "clientIP should be present in the cluster");
        ArrayList<ClientNode> clients = clientsIP.get(firstCluster);

        assertNotNull(clients, "clients should not be null");
        assertEquals(1, clients.size(), "Cluster should contain exactly one client node");

        ClientNode firstClient = clients.get(0);
        assertEquals("10.0.0.1", firstClient.hostName(), "Client hostname should match added IP");
        assertEquals(4562, firstClient.port(), "Client port should match added port");
    }

    @org.junit.jupiter.api.Test
    void addingMultipleUsers() throws NoSuchFieldException, IllegalAccessException {
        Topology topology = new Topology();

        topology.addUser("10.0.0.1", 4562);
        topology.addUser("10.0.0.2", 4563);
        topology.addUser("10.0.0.3", 4564);
        topology.addUser("10.0.0.4", 4565);

        int numClients = getPrivateField(topology, "numClients");
        int numClusters = getPrivateField(topology, "numClusters");
        ArrayList<Cluster> clusters = getPrivateField(topology, "clusters");
        HashMap<Cluster, ArrayList<ClientNode>> clientIP = getPrivateField(topology, "clientIP");

        assertEquals(4, numClients, "total clients should be 4");
        assertTrue(numClusters >= 1, "there should be atleast one cluster");

        int totalfoundclients = 0;
        for (Cluster c : clusters) {
            assertFalse(clientIP.get(c).isEmpty(), "No cluster should be empty");
            totalfoundclients += clientIP.get(c).size();
        }

        assertEquals(numClients, totalfoundclients, "total clients found across clusters should equal numClients");
    }

    /* this is helper utility to access private member of topology class for test
        required: no getter method for topology class
    */
    private <T> T getPrivateField(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(obj);
    }
}