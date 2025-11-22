package com.swe.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.assertEquals;

/**
 * Test class to test TCPCommunicator class.
 */
public class TCPCommunicatorTest {

    /**
     * Test for checking if message is sent correctly.
     */
    @org.junit.jupiter.api.Test
    public void testSend() {
        try {
            final int port1 = 8001;
            final int port2 = 8002;
            final Thread recieveThread1 = new Thread(() -> receive(port1));
            final Thread recieveThread2 = new Thread(() -> receive(port2));
            recieveThread1.start();
            recieveThread2.start();
            final Integer sleepTime = 500;
            Thread.sleep(sleepTime);
            final String localAddress = "10.32.0.41";
            final ProtocolBase tcp = new TCPCommunicator(8000);
            final String data = "Welcome to the new world!!!";
            final ClientNode dest = new ClientNode(localAddress, port1);
            final ClientNode dest1 = new ClientNode(localAddress, port2);
            tcp.sendData(data.getBytes(), dest);
            tcp.sendData(data.getBytes(), dest1);
            System.out.println("Data sent successfully...");
            tcp.closeSocket(dest);
            tcp.closeSocket(dest1);
            tcp.close();
            recieveThread1.join();
            recieveThread2.join();
        } catch (InterruptedException ex) {
            System.out.println("Send Error : " + ex.getMessage());
        }
    }

    /**
     * Sample function to receive data.
     *
     * @param serverPort the port to run
     */
    public void receive(final int serverPort) {
        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            serverSocket.setSoTimeout(0);
            final Socket socket = serverSocket.accept();
            final DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            final byte[] packet = dataIn.readAllBytes();
            System.out.println("Data received : " + new String(packet));
            assertEquals(new String(packet), "Welcome to the new world!!!");
        } catch (IOException ex) {
            System.out.println("Error : " + ex.getMessage());
        }
    }

    /**
     * Test for checking if message is received correctly.
     */
    @org.junit.jupiter.api.Test
    public void testReceive() {
        try {
            final int serverPort = 9900;
            final ProtocolBase tcp = new TCPCommunicator(serverPort);
            final Thread receiveThread = new Thread(() -> {
                byte[] data = new byte[100];
                data = tcp.receiveData();
                if (data != null) {
                    System.out.println("Received : " + new String(data));
                    assertEquals(new String(data), "Hello World !!!");
                }
                data = tcp.receiveData();
                if (data != null) {
                    System.out.println("Received : " + new String(data));
                    assertEquals(new String(data), "Hello World !!!");
                }
                data = tcp.receiveData();
                if (data != null) {
                    System.out.println("Received : " + new String(data));
                    assertEquals(new String(data), "Hello World !!!");
                }
            });
            receiveThread.start();
            send(serverPort);
            receiveThread.join();
            tcp.close();
        } catch (Exception ex) {
            System.out.println("Receive thread is interrupted...");
        }
    }

    /**
     * Sample function to receive data.
     *
     * @param serverPort the port to connect to
     */
    public void send(final int serverPort) {
        final Socket destSocket = new Socket();
        try {
            final Integer timeout = 5000;
            final String localAddress = "10.32.0.41";
            destSocket.connect(new InetSocketAddress(localAddress, serverPort), timeout);
            final DataOutputStream dataOut = new DataOutputStream(destSocket.getOutputStream());
            final String data = "Hello World !!!";
            dataOut.write(data.getBytes());
            System.out.println("Data sent successfully");
            destSocket.close();
        } catch (IOException ex) {
        }
    }

    @org.junit.jupiter.api.Test
    public void testErrorSetServerPort() {
        final ProtocolBase tcp = new TCPCommunicator(1);
        tcp.close();
    }

    @org.junit.jupiter.api.Test
    public void testErrorCloseSocket() {
        final ProtocolBase tcp = new TCPCommunicator(8011);
        final String localAddress = "10.32.0.41";
        final ClientNode client = new ClientNode(localAddress, 9000);
        tcp.closeSocket(client);
        tcp.close();
    }
}
