/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Scanner;

/**
 *
 * @author ab811
 */
public class TFTPClient {
    
    private static final String serverIpAddress = "localhost";
    private static final int portNumber = 4950;
    
    public static String ipAddress = serverIpAddress;
    private static final byte opRRQ =1;
    private static final byte opWRQ = 2;
    private static final byte opDATAPACKET = 3;
    private static final byte opACK = 4;
    private static final byte opERROR = 5;
    
    public static final int bufSize = 516;
    private static final int packetSize = 516;
    
    private DatagramSocket socket = null;
    private InetAddress inetAddress = null;
    private byte[] sendBuf;
    private byte[] receiveBuf;
    private DatagramPacket sendPacket;
    private DatagramPacket receivePacket;
    
    public static final String READDIR = ".\\";
	public static final String WRITEDIR = ".\\";
    
            
    public static void main(String [] args) throws IOException{
        
       // TFTPClient client = new TFTPClient();
        
        while(true){
            TFTPClient tFTPClientNet = new TFTPClient();
        
      int selection = menu();
           if(selection == 1){
              //downloadFile();
             // TFTPClient tFTPClientNet = new TFTPClient();
              System.out.println("Enter file name to download");
                      Scanner name = new Scanner(System.in);
                      
                   String fileName = name.next();
		
		tFTPClientNet.getFile(fileName);
              
          }else if(selection == 2){
             // TFTPClient tFTPClientNet = new TFTPClient();
              System.out.println("Enter file name to upload");
                      Scanner name = new Scanner(System.in);
                      
                   String fileName = name.next();
                   
              tFTPClientNet.sendFile(fileName);
    }else if(selection == 3){
        System.exit(0);
    }
           
        }  
        
        
        
    }
    
    
    
    public TFTPClient(){
        System.out.println("Enter 'localhost' if the server is run in your local network, else enter your IPv4 address : ");
        Scanner input = new Scanner(System.in);
        ipAddress = input.next();
    }
    
    
    private static int menu(){
        int selection;
        Scanner input = new Scanner(System.in);
        
        /****************************************/
        
        System.out.println("/****************************/");
        System.out.println("/*    Connected to server   */");
        System.out.println("/*Enter 1 to download a file*/");
        System.out.println("/*Enter 2 to upload a file* */");
        System.out.println("/*Enter 3 to exit           */");
        System.out.println("/****************************/");
        
        selection = input.nextInt();
        return selection;  
    }
    
 
    
    public void getFile(String fileName) throws IOException {
        inetAddress = InetAddress.getByName(ipAddress);
        socket = new DatagramSocket();
        sendBuf = createRequest(opRRQ, fileName, "octet");
        sendPacket = new DatagramPacket(sendBuf, sendBuf.length, inetAddress, portNumber);
        socket.send(sendPacket);
        ByteArrayOutputStream ByteOutOS = receiveFile(); 
        writeFile(ByteOutOS, fileName);
        
    }
    
    
    
    public  ByteArrayOutputStream receiveFile() throws IOException{
        ByteArrayOutputStream byteOutOS = new ByteArrayOutputStream();
		int block = 1;
		do {
			System.out.println("TFTP Packet count: " + block);
			block++;
			receiveBuf = new byte[packetSize];
			receivePacket = new DatagramPacket(receiveBuf,
					receiveBuf.length, inetAddress,
					socket.getLocalPort());
			
			//STEP 2.1: receive packet from TFTP server
			socket.receive(receivePacket);

			// Getting the first 4 characters from the TFTP packet
			byte[] opCode = { receiveBuf[0], receiveBuf[1] };

			if (opCode[1] == opERROR) {
				reportError();
			} else if (opCode[1] == opDATAPACKET) {
				// Check for the TFTP packets block number
				byte[] blockNumber = { receiveBuf[2], receiveBuf[3] };

				DataOutputStream dos = new DataOutputStream(byteOutOS);
				dos.write(receivePacket.getData(), 4,
						receivePacket.getLength() - 4);

				//STEP 2.2: send ACK to TFTP server for received packet
				sendAcknowledgment(blockNumber);
			}

		} while (!isLastPacket(receivePacket));
		return byteOutOS;
        
    }
    
    
    private void sendAcknowledgment (byte[] blockNumber){
        byte[] ACK = { 0, opACK, blockNumber[0], blockNumber[1] };

		// TFTP Server communicates back on a new PORT
		// so get that PORT from in bound packet and
		// send acknowledgment to it
		DatagramPacket ack = new DatagramPacket(ACK, ACK.length, inetAddress,
				receivePacket.getPort());
		try {
			socket.send(ack);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    private void reportError() {
		String errorCode = new String(receiveBuf, 3, 1);
		String errorText = new String(receiveBuf, 4,
				receivePacket.getLength() - 4);
		System.err.println("Error: " + errorCode + " " + errorText);
	}
    
    private void writeFile(ByteArrayOutputStream baoStream, String fileName) {
		try {
			OutputStream outputStream = new FileOutputStream(fileName);
			baoStream.writeTo(outputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    
    private boolean isLastPacket(DatagramPacket datagramPacket) {
		if (datagramPacket.getLength() < 512)
			return true;
		else
			return false;
	}
    
    private byte[] createRequest(final byte opCode, final String fileName,
			final String mode) {
		byte zeroByte = 0;
		int rrqByteLength = 2 + fileName.length() + 1 + mode.length() + 1;
		byte[] rrqByteArray = new byte[rrqByteLength];

		int position = 0;
		rrqByteArray[position] = zeroByte;
		position++;
		rrqByteArray[position] = opCode;
		position++;
		for (int i = 0; i < fileName.length(); i++) {
			rrqByteArray[position] = (byte) fileName.charAt(i);
			position++;
		}
		rrqByteArray[position] = zeroByte;
		position++;
		for (int i = 0; i < mode.length(); i++) {
			rrqByteArray[position] = (byte) mode.charAt(i);
			position++;
		}
		rrqByteArray[position] = zeroByte;
		return rrqByteArray;
	}
    
    
    /**
     * This method creates and sends a WRQ and then sends the file data
     * @param fileName
     * @throws IOException 
     */
    private void sendFile(String fileName) throws IOException{
        inetAddress = InetAddress.getByName(ipAddress);
        socket = new DatagramSocket();
        sendBuf = createRequest(opWRQ, fileName, "octet");
        sendPacket = new DatagramPacket(sendBuf, sendBuf.length, inetAddress, portNumber);
        socket.send(sendPacket);
        uploadFile(socket, fileName);
        
        
        
    } 
    
    /**
     * This method creates a data packet.
     * @param block
     * @param data
     * @param length
     * @return 
     */
    private DatagramPacket dataPacket(short block, byte[] data, int length) {
		
	ByteBuffer buffer = ByteBuffer.allocate(bufSize);
        buffer.putShort(opDATAPACKET);
        buffer.putShort(block);
        buffer.put(data, 0, length);
		
        return new DatagramPacket(buffer.array(), 4+length);
	} 
    
    /**
     * This methods receives acknowledgment from the server
     * @param ack
     * @return 
     */
    private short getAck(DatagramPacket ack) {
		ByteBuffer buffer = ByteBuffer.wrap(ack.getData());
		return buffer.getShort();
	}

    /**
     * This method sends the file data.
     * @param sendSocket
     * @param sender
     * @param blockNum
     * @return
     * @throws SocketException
     * @throws IOException 
     */
    private boolean WriteAndReadAck(DatagramSocket sendSocket, DatagramPacket sender, short blockNum) throws SocketException, IOException {
		int retryCount = 0;
		byte[] rec = new byte[bufSize];
		DatagramPacket receiver = new DatagramPacket(rec, rec.length);
		
		while(true) {
			if (retryCount >= 6) {
	            System.err.println("Timed out.");
	            return false;
	        }
	        try {
	            sendSocket.send(sender);
	            System.out.println("Sent.");
	            sendSocket.setSoTimeout(((int) Math.pow(2, retryCount++))*1000);
	            sendSocket.receive(receiver);
	            
	            short ack = getAck(receiver);
//	            System.out.println("Ack received: " + ack);
	            if (ack == blockNum) {
//	            	System.out.println("Received correct OP_ACK");
	            	return true;
	            } else if (ack == -1) {
	            	return false;
	            } else {
//	            	System.out.println("Ignore. Wrong ack.");
	            	retryCount = 0;
	            	throw new SocketTimeoutException();
	            }
	            
	        } catch (SocketTimeoutException e) {
	            System.out.println("Timeout. Resending.");
	        } 
		}
	}

    
    
    
    
    private void uploadFile(DatagramSocket sendSocket, String string){
        
        String filepath = READDIR + string;
        System.out.println(filepath);
		File file = new File(filepath);
		byte[] buf = new byte[bufSize-4];
                
        FileInputStream in = null;
			try {
				in = new FileInputStream(file);

				
		short blockNum = 1;
			
			while (true) {
				
				int length;
				
					length = in.read(buf);
				
				
				if (length == -1) {
					length = 0;
				}
				DatagramPacket sender = dataPacket(blockNum, buf, length);
                                sender.setAddress(inetAddress);
                                sender.setPort(portNumber);
				System.out.println("Sending.........");
				if (WriteAndReadAck(sendSocket, sender, blockNum++)) {
					System.out.println("Success. Send another. blockNum = " + blockNum);
				}
				
				if (length < 512) {
					
						in.close();
					
					break;
				}
			}
                        	} catch (IOException e) {
				System.err.println("File not found. Sending error packet.");
				
				return;
			}
    }
        
        
   
    
    
    
    
    

   
   

   
}
