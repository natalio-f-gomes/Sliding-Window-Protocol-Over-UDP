package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import Common.MyLogger;
import Common.Settings;

public class UDP {
    private MyLogger logger = new MyLogger("Server.UDP");

    public void createService() throws SocketException {
        logger.info((restarted ? "Res" : "S") + "starting Server on Port: " + Settings.PORT);
        
        restarted = true;
        serverSocket = new DatagramSocket(Settings.PORT);
    }

    public byte[] getPacket(int timeout) throws SocketTimeoutException {
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        try {
            serverSocket.setSoTimeout(timeout);
            
            serverSocket.receive(receivePacket);
            
            logger.received(receiveData);
            
            if (clientPort == 0) {
                clientAddress = receivePacket.getAddress();
                clientPort = receivePacket.getPort();

                logger.info("Established connection with: " + clientAddress + ", Port:" + clientPort);
            }
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException e) {
            logger.error("Unable to get data from socket.", e);
        }
        return receiveData;
    }

    public void sendPacket(int size, byte[] sendData) {
        totalData += size;
        totalDataWithHdr += size + 12;

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
        try {
            logger.sent(sendData);
            
            serverSocket.send(sendPacket);
        } catch (IOException e) {
            logger.error("Unable to send reply", e);
        }
    }

    public void reset() {
        restarted = true;
        clientPort = 0;
    }
    
    public void close() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }
    
    public void stats() {
        logger.info(String.format("    Total Data:                          %4d", totalData));
        logger.info(String.format("    Total Data Including Packet Headers: %4d", totalDataWithHdr));        
    }

    private DatagramSocket serverSocket;
    private int clientPort = 0;
    private InetAddress clientAddress;
    
    private static boolean restarted = false;
    private static int totalData = 0;
    private static int totalDataWithHdr = 0;
}