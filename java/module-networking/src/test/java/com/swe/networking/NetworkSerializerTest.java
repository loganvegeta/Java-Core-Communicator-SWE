package com.swe.networking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** Test class for serializers. */
public class NetworkSerializerTest {
    /**
     * Function to test serializing clientNetworkRecord.
     */
    @org.junit.jupiter.api.Test
    public void clientNetworkRecordTest() {
        final ClientNode client = new ClientNode("127.0.0.1", 1234);
        final ClientNetworkRecord record = new ClientNetworkRecord(client, 1);
        final NetworkSerializer serializer = NetworkSerializer.getNetworkSerializer();
        final byte[] data = serializer.serializeClientNetworkRecord(record);
        System.out.println(Arrays.toString(data));
        final ClientNetworkRecord data1 = serializer.deserializeClientNetworkRecord(data);
        System.out.println(data1);
        assertEquals(record, data1);
    }

    /**
     * Function to test serializing Client Record.
     */
    @org.junit.jupiter.api.Test
    public void clientNodeTest() {
        final ClientNode client = new ClientNode("127.0.0.1", 1234);
        final NetworkSerializer serializer = NetworkSerializer.getNetworkSerializer();
        final byte[] data = serializer.serializeClientNode(client);
        System.out.println(Arrays.toString(data));
        final ClientNode data1 = serializer.deserializeClientNode(data);
        System.out.println(data1);
        assertEquals(client, data1);
    }

    /**
     * Function to test serializing NetworkStructure.
     */
    @org.junit.jupiter.api.Test
    public void networkStructureTest() {
        final ClientNode client = new ClientNode("127.0.0.1", 1234);
        final ClientNode client1 = new ClientNode("127.0.0.1", 1235);
        final ClientNode client2 = new ClientNode("127.0.0.1", 1236);
        final ClientNode client3 = new ClientNode("127.0.0.1", 1237);
        final ClientNode client4 = new ClientNode("127.0.0.1", 1238);

        final List<ClientNode> cluster1 = new ArrayList<>();
        cluster1.add(client);
        cluster1.add(client1);
        final List<ClientNode> cluster2 = new ArrayList<>();
        cluster2.add(client2);
        cluster2.add(client3);
        cluster2.add(client4);
        final List<List<ClientNode>> clusters = new ArrayList<>();
        clusters.add(cluster1);
        clusters.add(cluster2);
        final List<ClientNode> servers = new ArrayList<>();
        servers.add(client);
        servers.add(client2);
        final NetworkStructure network = new NetworkStructure(clusters, servers);

        final NetworkSerializer serializer = NetworkSerializer.getNetworkSerializer();
        final byte[] data = serializer.serializeNetworkStructure(network);
        System.out.println(Arrays.toString(data));
        final NetworkStructure data1 = serializer.deserializeNetworkStructure(data);
        System.out.println(data1);
        assertEquals(network, data1);
    }
}
