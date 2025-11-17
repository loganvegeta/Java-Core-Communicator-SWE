package com.swe.networking;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

//File owned by Loganath
/**
 * Class object for the Network RPC.
 */
public class NetworkRPC {

    /**
     * Private constructor for networkRPC.
     */
    private static NetworkRPC networkRPC = null;
    /**
     * Variable to store the networking.
     */
    private static Networking networking;

    private NetworkRPC() {
        System.out.println("Network RPC...");
    }

    /**
     * Function to get hte singleton RPC class object.
     *
     * @return the singleton object
     */
    public static NetworkRPC getNetworkRPC() {
        if (networkRPC == null) {
            networkRPC = new NetworkRPC();
            networking = Networking.getNetwork();
            System.out.println("Instantiating new Network RPC...");
        }
        System.out.println("Passing Network RPC...");
        return networkRPC;
    }

    /**
     * Network RPC function equal to addUser.
     *
     * @param args the data received
     * @return null for matchin rpc arguments.
     */
    public byte[] networkRPCAddUser(final byte[] args) {
        final ByteBuffer buffer = ByteBuffer.wrap(args);

        final byte deviceHostLen = buffer.get();
        final byte[] deviceHostBytes = new byte[deviceHostLen];
        buffer.get(deviceHostBytes);
        final String deviceHost = new String(deviceHostBytes, StandardCharsets.UTF_8);
        final int devicePort = buffer.getInt();
        final ClientNode deviceAddress = new ClientNode(deviceHost, devicePort);

        final byte serverHostLen = buffer.get();
        final byte[] serverHostBytes = new byte[serverHostLen];
        buffer.get(serverHostBytes);
        final String serverHost = new String(serverHostBytes, StandardCharsets.UTF_8);
        final int serverPort = buffer.getInt();
        final ClientNode mainServerAddress = new ClientNode(serverHost, serverPort);

        networking.addUser(deviceAddress, mainServerAddress);
        return null;
    }

    /**
     * Network RPC function equal to removeSubscription.
     *
     * @param args the data received
     * @return null for matchin rpc arguments.
     */
    public byte[] networkRPCRemoveSubscription(final byte[] args) {
        final ByteBuffer buffer = ByteBuffer.wrap(args);
        final int module = buffer.getInt();

        networking.removeSubscription(module);
        return null;
    }

    /**
     * Network RPC function equal to Subscribe.
     *
     * @param args the data received
     * @return null for matchin rpc arguments.
     */
    public byte[] networkRPCSubscribe(final byte[] args) {
        final ByteBuffer buffer = ByteBuffer.wrap(args);
        final int module = buffer.getInt();

        networking.subscribe(module, (byte[] data) -> {
            final int bufferSize = data.length + Integer.BYTES;
            final ByteBuffer callBuffer = ByteBuffer.allocate(bufferSize);
            callBuffer.putInt(module);
            callBuffer.put(data);
            // call rpc our callSubscriber function in Frontend
            // networkFrontCallSubscriber(int module,byte[] data)
        });
        return null;
    }

    /**
     * Network RPC function equal to Broadcast.
     *
     * @param args the data received
     * @return null for matchin rpc arguments.
     */
    public byte[] networkRPCBroadcast(final byte[] args) {
        final ByteBuffer buffer = ByteBuffer.wrap(args);
        final int dataLength = buffer.getInt();

        final byte[] data = new byte[dataLength];
        buffer.get(data);
        final int module = buffer.getInt();
        final int priority = buffer.getInt();
        networking.broadcast(data, module, priority);
        return null;
    }

    /**
     * Network RPC function equal to SendData.
     *
     * @param args the data received
     * @return null for matchin rpc arguments.
     */
    public byte[] networkRPCSendData(final byte[] args) {
        final ByteBuffer buffer = ByteBuffer.wrap(args);

        final int destCount = buffer.getInt();
        final ClientNode[] dest = new ClientNode[destCount];
        for (int i = 0; i < destCount; i++) {
            final byte hostLen = buffer.get();
            final byte[] hostBytes = new byte[hostLen];
            buffer.get(hostBytes);
            final String hostName = new String(hostBytes, StandardCharsets.UTF_8);
            final int port = buffer.getInt();
            dest[i] = new ClientNode(hostName, port);
        }

        final int dataLength = buffer.getInt();
        final byte[] data = new byte[dataLength];
        buffer.get(data);
        final int module = buffer.getInt();
        final int priority = buffer.getInt();

        networking.sendData(data, dest, module, priority);
        return null;
    }

    /**
     * Function to close the networking module.
     *
     * @param args the arguments passed
     * @return null for matching rpc arguments
     */
    public byte[] networkRPCCloseNetworking(final byte[] args) {
        networking.closeNetworking();
        return null;
    }

    /**
     * function to call back in the frontend.
     *
     * @param module the module to call
     * @param data the data to be passed
     */
    public void networkRPCCallSubscriber(final int module, final byte[] data) {
        networking.callSubscriber(module, data);
    }

}
