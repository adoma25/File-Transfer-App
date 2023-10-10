/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import static sun.rmi.transport.DGCAckHandler.received;

/**
 *
 * @author ab811
 */
public class TFTPServer extends Thread{
        public static final int TFTUDPPORT = 4950;
	public static final int BUFSIZE = 516;
        
    private byte[] sendBuf;
    private byte[] receiveBuf;
    private DatagramPacket sendPacket;
    private DatagramPacket receivePacket;
    private DatagramSocket socket = null;
    private InetAddress inetAddress = null;
    public static final int packetSize = 516;
        

	public static final String READDIR = ".\\";
	public static final String WRITEDIR = ".\\";

	public static final short opRRQ = 1;
	public static final short opWRQ = 2;
	public static final short opDAT = 3;
	public static final short opACK = 4;
	public static final short opERR = 5;

	public static final short ERR_FNF = 1;

	public static String 	  mode;


    
    @Override
    public void run(){
        byte[] buf= new byte[BUFSIZE];
		
		//Create socket
		DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(null);
        } catch (SocketException ex) {
            Logger.getLogger(TFTPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
		
		
		SocketAddress localBindPoint= new InetSocketAddress(TFTUDPPORT);
        try {
            socket.bind(localBindPoint);
        } catch (SocketException ex) {
            Logger.getLogger(TFTPServer.class.getName()).log(Level.SEVERE, null, ex);
        }

		System.out.printf("Waiting for new requests on port: "+TFTUDPPORT+ "\n");

                //Runs infinit loop
		while(true) {       
			final InetSocketAddress clientAddress = 
				receiveFrom(socket, buf);
			if (clientAddress == null) 
				continue;

			final StringBuffer requestedFile = new StringBuffer();
			final int requestType = parseRequest(buf, requestedFile);

			new Thread() {
				public void run() {
					try {
						DatagramSocket sendSocket = new DatagramSocket(0);
						sendSocket.connect(clientAddress);
												
						  

						if (requestType == opRRQ) {     
                                                    System.out.printf("Reading request for" + requestedFile.toString() + " from " + clientAddress.getHostName() + " using port " + clientAddress.getPort() + "\n");
							requestedFile.insert(0, READDIR);
							HandleRequest(sendSocket, requestedFile.toString(), opRRQ);
						}
						else {                       
                                                     System.out.printf("Writing request for" + requestedFile.toString() + " from " + clientAddress.getHostName() + " using port " + clientAddress.getPort() + "\n");
                                                    String fileName = requestedFile.toString();
							requestedFile.insert(0, WRITEDIR);
							//HandleRQ(sendSocket,requestedFile.toString(),opWRQ);
                                                        ByteArrayOutputStream ByteOutOS = receiveFile1();
                                                        writeFile(ByteOutOS, fileName);
						}
						sendSocket.close();
					} catch (SocketException e) {
						e.printStackTrace();
					} catch (IOException ex) {
                                        Logger.getLogger(TFTPServer.class.getName()).log(Level.SEVERE, null, ex);
                                    }
				}
			}.start();
		}
    }
    
        public static void main(String[] args) throws IOException {
        if (args.length > 0) {
			System.err.printf("usage: java %s\n", TFTPServer.class.getCanonicalName());
			System.exit(1);
		}
        TFTPServer server= new TFTPServer();
        server.start();
    }
    
        
        
        
        
        private void writeFile(ByteArrayOutputStream baoStream, String fileName) {
		try {
			OutputStream outputStream = new FileOutputStream(fileName);
			baoStream.writeTo(outputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
        public  ByteArrayOutputStream receiveFile1() throws IOException{
        ByteArrayOutputStream byteOutOS = new ByteArrayOutputStream();
		int block = 1;
		do {
			System.out.println("TFTP Server sent packet number: " + block);
			block++;
			receiveBuf = new byte[BUFSIZE];
			receivePacket = new DatagramPacket(receiveBuf,
					receiveBuf.length, inetAddress,
					socket.getLocalPort());
			
			//Receiving packet from client
			socket.receive(receivePacket);

		
			byte[] opCode = { receiveBuf[0], receiveBuf[1] };

			if (opCode[1] == opERR) {
				
                                System.err.println("ss");
			} else if (opCode[1] == opDAT) {
				
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

		
		DatagramPacket ack = new DatagramPacket(ACK, ACK.length, inetAddress,
				receivePacket.getPort());
		try {
			socket.send(ack);
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
        
        
        private InetSocketAddress receiveFrom(DatagramSocket socket, byte[] buf) {
		DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
		
		try {
			socket.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		InetSocketAddress client = new InetSocketAddress(receivePacket.getAddress(),receivePacket.getPort());
		
		return client;
	}
        
        private short parseRequest(byte[] buf, StringBuffer requestedFile) {
		ByteBuffer wrap = ByteBuffer.wrap(buf);
		short opcode = wrap.getShort();
		int delimiter = -1;
		for (int i = 2; i < buf.length; i++) {
			if (buf[i] == 0) {
				delimiter = i;
				break;
			}
		}
		
		if (delimiter == -1) {
			System.err.println("Corrupt request packet. Shutting down I guess.");
			System.exit(1);
		}
		
		String fileName = new String(buf, 2, delimiter-2);
//		System.out.println("Requested file = " + fileName);
		requestedFile.append(fileName);
		
		for (int i = delimiter+1; i < buf.length; i++) {
			if (buf[i] == 0) {
				String temp = new String(buf,delimiter+1,i-(delimiter+1));
//				System.out.println("Transfer mode = " + temp);
				mode = temp;
				if (temp.equalsIgnoreCase("octet")) {
					return opcode;
				} else {
					System.err.println("No mode specified.");
					System.exit(1);
				}
			}
		}
		System.err.println("Did not find delimiter.");
		System.exit(1);
		return 0;
	}
        
        private void HandleRequest(DatagramSocket sendSocket, String string, int opRrq) throws IOException {
		System.out.println(string);
		File file = new File(string);
		byte[] buf = new byte[BUFSIZE-4];
		
		if (opRrq == opRRQ) {
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
				System.out.println("Sending.........");
				if (sendFile(sendSocket, sender, blockNum++)) {
					System.out.println("Sent Successfuly blockNum = " + blockNum);
				}
				
				if (length < 512) {
					
						in.close();
					
					break;
				}
			}
                        	} catch (IOException e) {
				System.err.println("File not found. Sending error packet.");
				sendError(sendSocket, ERR_FNF, "");
				return;
			}
		} else if (opRrq == opWRQ) {			
				FileOutputStream output = null;
				
					output = new FileOutputStream(file);
				
				
				short blockNum = 0;
				
				while (true) {
					DatagramPacket dataPacket = ReadAndWriteData(sendSocket, acknowledgmentPacket(blockNum++), blockNum);
					
					 
						byte[] data = dataPacket.getData();
						
							output.write(data, 4, dataPacket.getLength()-4);
							System.out.println(dataPacket.getLength());
						
						if (dataPacket.getLength()-4 < 512) {
						
								sendSocket.send(acknowledgmentPacket(blockNum));
							
							System.out.println("All done writing file.");
							
								output.close();
							
							break;
						}
					
				}
			 
		} else {
			System.err.println("Um... I do not know what to do now so I will stop.");
		}
		
	} 
        
        private DatagramPacket ReadAndWriteData(DatagramSocket sendSocket, DatagramPacket sendAck, short block) {
		int retryCount = 0;
		byte[] rec = new byte[BUFSIZE];
		DatagramPacket receiver = new DatagramPacket(rec, rec.length);

        while(true) {
            if (retryCount >= 6) {
                System.err.println("Timed out. Closing connection.");
                return null;
            }
            try {
            	System.out.println("sending ack for block: " + block);
            	sendSocket.send(sendAck);
                sendSocket.setSoTimeout(((int) Math.pow(2, retryCount++))*1000);
                sendSocket.receive(receiver);
                
                short blockNum = receiveData(receiver);
                System.out.println(blockNum + " " + block);
                if (blockNum == block) {
                	return receiver;
                } else if (blockNum == -1) {
                	return null;
                } else {
                	System.out.println("Duplicate.");
                	retryCount = 0;
                	throw new SocketTimeoutException();
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout.");
                try {
					sendSocket.send(sendAck);
				} catch (IOException e1) {
					System.err.println("Error sending...");
				}
            } catch (IOException e) {
				System.err.println("IO Error.");
			} finally {
                try {
					sendSocket.setSoTimeout(0);
				} catch (SocketException e) {
					System.err.println("Error resetting Timeout.");
				}
            }
        }
	}
        
        private boolean sendFile(DatagramSocket sendSocket, DatagramPacket sender, short blockNum) throws IOException {
		int retryCount = 0;
		byte[] rec = new byte[BUFSIZE];
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
	            

	            short ack = getAcknowledgment(receiver);
//	            System.out.println("Ack received: " + ack);
	            if (ack == blockNum) {
//	            	System.out.println("Received correct opACK");
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
        
        private DatagramPacket acknowledgmentPacket(short block) {
		
		ByteBuffer buffer = ByteBuffer.allocate(BUFSIZE);
        buffer.putShort(opACK);
        buffer.putShort(block);
		
        return new DatagramPacket(buffer.array(), 4);
	}
        
        private DatagramPacket dataPacket(short block, byte[] data, int length) {
		
        ByteBuffer buffer = ByteBuffer.allocate(BUFSIZE);
        buffer.putShort(opDAT);
        buffer.putShort(block);
        buffer.put(data, 0, length);
		
        return new DatagramPacket(buffer.array(), 4+length);
	}
        
        private short getAcknowledgment(DatagramPacket ack) {
		ByteBuffer buffer = ByteBuffer.wrap(ack.getData());
		short opcode = buffer.getShort();
		
		
		return buffer.getShort();
	} 
        
        private short receiveData(DatagramPacket data) {
		ByteBuffer buffer = ByteBuffer.wrap(data.getData());
		short opcode = buffer.getShort();
		
		
		return buffer.getShort();
	}
        
        private void sendError(DatagramSocket sendSocket, short errorCode, String errMsg) throws IOException {
		
		ByteBuffer bBuff = ByteBuffer.allocate(BUFSIZE);
		bBuff.putShort(opERR);
		bBuff.putShort(errorCode);
		bBuff.put(errMsg.getBytes());
		bBuff.put((byte) 0);
		
		DatagramPacket receivePacket = new DatagramPacket(bBuff.array(),bBuff.array().length);
		
			sendSocket.send(receivePacket);
		
		
	}
        
      
    
}
