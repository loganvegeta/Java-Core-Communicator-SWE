package com.swe.networking;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class for serializing network objects.
 */
public class NetworkSerializer {
    /**
     * variable to store the singleton classobject.
     */
    private static NetworkSerializer serializer = null;

    /**
     * Constructor class network serializer class.
     */
    private NetworkSerializer() {

    }

    /**
     * Function to return the static serializer class.
     *
     * @return the singleton object
     */
    public static NetworkSerializer getNetworkSerializer() {
        if (serializer == null) {
            System.out.println("Creating new Network Serializer object...");
            serializer = new NetworkSerializer();
            return serializer;
        }
        System.out.println("Passing already instantiated Network Serializer object...");
        return serializer;
    }

    /**
     * Function to serialize the clientNetworkRecord datatype.
     *
     * @param record The object to serialize.
     * @return the serialized output
     */
    public byte[] serializeClientNetworkRecord(final ClientNetworkRecord record) {
        final byte[] hostName = record.client().hostName().getBytes(StandardCharsets.UTF_8);
        final int bufferSize = Integer.BYTES + hostName.length + Integer.BYTES + Integer.BYTES;
        final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.putInt(hostName.length);
        buffer.put(hostName);
        buffer.putInt(record.client().port());
        buffer.putInt(record.clusterIndex());
        return buffer.array();
    }

    /**
     * Function to deserialize ClientNetworkRecord.
     *
     * @param data the data to deserialized
     * @return the ClientNetworkRecord object
     */
    public ClientNetworkRecord deserializeClientNetworkRecord(final byte[] data) {
        final ByteBuffer buffer = ByteBuffer.wrap(data);

        if (buffer.remaining() < Integer.BYTES + Integer.BYTES + Integer.BYTES) {
            throw new IllegalArgumentException("Data too short ");
        }

        final int hostLength = buffer.getInt();

        if (hostLength < 0) {
            throw new IllegalArgumentException("Negative host length: " + hostLength);
        }

        final int needed = hostLength + Integer.BYTES + Integer.BYTES;
        if (buffer.remaining() < needed) {
            throw new IllegalArgumentException("Not enough bytes to read");
        }

        final byte[] hostName = new byte[hostLength];
        buffer.get(hostName);
        final String hostIp = new String(hostName, StandardCharsets.UTF_8);
        final int hostPort = buffer.getInt();
        final int clusterIdx = buffer.getInt();
        final ClientNetworkRecord record = new ClientNetworkRecord(new ClientNode(hostIp, hostPort), clusterIdx);
        return record;
    }

    /**
     * Function to serialize the clientNetworkRecord datatype.
     *
     * @param record The object to serialize.
     * @return the serialized output
     */
    public byte[] serializeClientNode(final ClientNode record) {
        final byte[] hostName = record.hostName().getBytes(StandardCharsets.UTF_8);
        final int bufferSize = 1 + hostName.length + Integer.BYTES;
        final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.put((byte) hostName.length);
        buffer.put(hostName);
        buffer.putInt(record.port());
        return buffer.array();
    }

    /**
     * Function to deserialize ClientNode.
     *
     * @param data the data to desrialized
     * @return the ClientNode object
     */
    public ClientNode deserializeClientNode(final byte[] data) {
        final ByteBuffer buffer = ByteBuffer.wrap(data);
        final int hostLength = buffer.get();
        final byte[] hostName = new byte[hostLength];
        buffer.get(hostName);
        final String hostIp = new String(hostName, StandardCharsets.UTF_8);
        final int hostPort = buffer.getInt();
        final ClientNode record = new ClientNode(hostIp, hostPort);
        return record;
    }

    /**
     * Function to deserialize ClientNode.
     *
     * @param structure the data to serialize
     * @return the serialized object
     */
    public byte[] serializeNetworkStructure(final NetworkStructure structure) {
        // Assume maximum 80 clients in 8 clusters
        // 1 Client -> max 10 bytes total -> 800 bytes
        final int bufferSize = 2000;
        final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.put((byte) structure.clusters().size());
        for (List<ClientNode> clusters : structure.clusters()) {
            buffer.put((byte) clusters.size());
            for (ClientNode client : clusters) {
                buffer.put(serializeClientNode(client));
            }
        }
        buffer.put((byte) structure.servers().size());
        for (ClientNode server : structure.servers()) {
            buffer.put(serializeClientNode(server));
        }
        final byte[] data = Arrays.copyOf(buffer.array(), buffer.position());
        return data;
    }

    /**
     * Function to deserialize NetworkStrucuture.
     *
     * @param data the data to deserialized
     * @return the Network Structure object
     */
    public NetworkStructure deserializeNetworkStructure(final byte[] data) {
        final ByteBuffer buffer = ByteBuffer.wrap(data);
        final int clustersLength = buffer.get();
        final List<List<ClientNode>> clusters = new ArrayList<>();
        for (int i = 0; i < clustersLength; i++) {
            final int clientsLength = buffer.get();
            final List<ClientNode> clients = new ArrayList<>();
            for (int j = 0; j < clientsLength; j++) {
                final int hostLength = buffer.get();
                final byte[] hostName = new byte[hostLength];
                buffer.get(hostName);
                final String hostIp = new String(hostName, StandardCharsets.UTF_8);
                final int hostPort = buffer.getInt();
                final ClientNode record = new ClientNode(hostIp, hostPort);
                clients.add(record);
            }
            clusters.add(clients);
        }
        final List<ClientNode> servers = new ArrayList<>();
        final int serversLength = buffer.get();
        for (int i = 0; i < serversLength; i++) {
            final int hostLength = buffer.get();
            final byte[] hostName = new byte[hostLength];
            buffer.get(hostName);
            final String hostIp = new String(hostName, StandardCharsets.UTF_8);
            final int hostPort = buffer.getInt();
            final ClientNode record = new ClientNode(hostIp, hostPort);
            servers.add(record);
        }
        final NetworkStructure network = new NetworkStructure(clusters, servers);
        return network;
    }
}
