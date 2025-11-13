package com.swe.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

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
            int port1 = 8001;
            int port2 = 8002;
            final Thread recieveThread1 = new Thread(() -> receive(port1));
            final Thread recieveThread2 = new Thread(() -> receive(port2));
            recieveThread1.start();
            recieveThread2.start();
            final Integer sleepTime = 500;
            Thread.sleep(sleepTime);
            final ProtocolBase tcp = new TCPCommunicator(8000);
            final String data = "Welcome to the new world!!!";
            final ClientNode dest = new ClientNode("10.128.12.13", port1);
            // TODO: Write test cases to cause connection errors
            final ClientNode dest1 = new ClientNode("127.0.0.1", port2);
            tcp.sendData(data.getBytes(), dest);
            tcp.sendData(data.getBytes(), dest1);
            tcp.sendData(data.getBytes(), dest);
            System.out.println("Data sent successfully...");
            tcp.close();
            tcp.closeSocket(dest);
            tcp.closeSocket(dest1);
            recieveThread1.join();
            recieveThread2.join();
        } catch (InterruptedException ex) {
        }
    }

    /**
     * Sample function to receive data.
     */
    public void receive(int serverPort) {
        try {
            final ServerSocket serverSocket = new ServerSocket(serverPort);
            serverSocket.setSoTimeout(0);
            final Socket socket = serverSocket.accept();
            final DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            final byte[] packet = dataIn.readAllBytes();
            System.out.println("Data received : " + new String(packet));
            serverSocket.close();
        } catch (IOException ex) {
        }
    }

    /**
     * Test for checking if message is received correctly.
     */
    @org.junit.jupiter.api.Test
    public void testReceive() {
        try {
            final ProtocolBase tcp = new TCPCommunicator(8001);
            // final Thread receiveThread = new Thread(() -> {
            //     byte[] data = new byte[100];
            //     data = tcp.receiveData();
            //     if (data != null)
            //         System.out.println("Received : " + new String(data));
            //     data = tcp.receiveData();
            //     if (data != null)
            //         System.out.println("Received : " + new String(data));
            //     data = tcp.receiveData();
            //     if (data != null)
            //         System.out.println("Received : " + new String(data));
            // });
            // receiveThread.start();
            // send();
            // receiveThread.join();
            byte[] data = new byte[1000];
            while (true) {
                data = tcp.receiveData();
                if (data != null) {
                    System.out.println("Data receievd: " + data);
                } else {
                    System.out.println("FInised loop...");
                    break;
                }
            }
        } catch (Exception ex) {
            System.out.println("Receive thread is interrupted...");
        }
    }

    /**
     * Sample function to receive data.
     */
    public void send() {
        final Socket destSocket = new Socket();
        try {
            final Integer port = 9000;
            final Integer timeout = 5000;
            destSocket.connect(new InetSocketAddress("127.0.0.1", port), timeout);
            final DataOutputStream dataOut = new DataOutputStream(destSocket.getOutputStream());
            final String data = "Hello World !!!";
            dataOut.write(data.getBytes());
            destSocket.close();
        } catch (IOException ex) {
        }
    }
}
