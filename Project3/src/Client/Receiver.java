package Client;

import java.io.*;
import java.net.*;

import Common.MyLogger;
import Common.Packet;
import Common.PacketType;
import Common.Settings;

public class Receiver {
    private MyLogger logger = new MyLogger("Client.Receiver");

    public void getFile(String fn) {
        Packet packet;
        
        packet = new Packet(PacketType.REQ, 0, fn.length(), fn.getBytes());

        try {
            receiveFile(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void sendAck(int sequenceNumber, InetAddress IPAddress, DatagramSocket clientSocket) throws IOException {
        Packet packet = new Packet(PacketType.ACK, sequenceNumber, 0, new byte[0]);
        
        DatagramPacket sendPacket = new DatagramPacket(
            packet.getBytes(),
            packet.getBytes().length,
            IPAddress,
            Settings.PORT
        );
        
        clientSocket.send(sendPacket);
        
        logger.sent(packet.getBytes());
    }
    
    private void receiveFile(Packet packet) throws Exception {
        byte[] packetBytes = packet.getBytes();

        byte[] receiveData = new byte[1000];
        
        DatagramSocket clientSocket = new DatagramSocket();
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        String fileBeingTransferred = "";
        
        int packetsReceived = 0;
        int nextSeqno = 0;
        int lastSeqno = 0;
        int totalData = 0;

        clientSocket.setSoTimeout(ONE_SECOND_IN_MS);

        InetAddress IPAddress = InetAddress.getByName(LOCALHOST);
        
        DatagramPacket sendPacket = new DatagramPacket(
            packetBytes,
            packetBytes.length,
            IPAddress,
            Settings.PORT
        );
        
        clientSocket.send(sendPacket);
        
        logger.sent(packetBytes);

        boolean haveEOT = false;
        while (!haveEOT) {
            try {
                Packet incomingData;
                
                clientSocket.receive(receivePacket);
                
                logger.received(receiveData);
                
                incomingData = new Packet(receiveData);

                if (incomingData.getType() == PacketType.ERR) {
                    logger.info("Error: " + new String(incomingData.getData()));
                    return;
                }

                if (incomingData.getSeqNo() == nextSeqno) {
                    if (incomingData.getType() != PacketType.EOT) {
                        fileBeingTransferred += new String(incomingData.getData(), 0, incomingData.getSize());
                    }
                    
                    totalData += incomingData.getSize();
                    
                    if (incomingData.getType() == PacketType.EOT) {
                        haveEOT = true;
                    }
                    
                    lastSeqno = incomingData.getSeqNo();
                    
                    sendAck(incomingData.getSeqNo(), IPAddress, clientSocket);
                    
                    nextSeqno = (nextSeqno + 1) % Settings.WINDOW_SIZE;
                    
                    packetsReceived++;
                    
                } else if (packetsReceived == 0) {
                    // do nothing - stale packet from a previous session
                    
                } else {
                    logger.info("Received a packet, but not the one we're expecting.");
                    
                    sendAck(lastSeqno, IPAddress, clientSocket);
                }
            } catch (SocketTimeoutException e) {
                logger.info("Receive timeout");
            }
        }
    
        clientSocket.close();
        logger.info("Received the file containing: " + totalData + "(bytes) of data");
        System.out.println(fileBeingTransferred);
    }

    private static int ONE_SECOND_IN_MS = 1000;
    private static String LOCALHOST = "localhost";
}