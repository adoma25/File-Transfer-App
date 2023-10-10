/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tftp.tcp.server;

import java.net.*;
import java.io.*;
import static java.lang.System.out;

/**
 *
 * @author ab811
 */
public class TFTPTCPServer {
    
    /**
    * TFTP Server port and Buf size.
    */
    private static final int TFTPTCPPORT = 6789;
    private static final int BUFSIZE = 516;
    
    /**
     * Writing and reading paths.
     */
    private static final String READDIR = ".\\";
    private static final String WRITEDIR = ".\\";
    
    /**
     * OP codes.
     */
    private static final int OP_RRQ = 1;
    private static final int OP_WRQ = 2;
    private static final int OP_DAT = 3;
  //  private static final short OP_ACK = 4;
    private static final int OP_ERR = 5;

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException{
        // TODO code application logic here
         
         ServerSocket welcomeSocket = new ServerSocket(TFTPTCPPORT);
         System.out.printf("Listening at port %d for new requests\n", TFTPTCPPORT);

         //Runs an infinit loop
            while (true) {
                
                //Establish a connection with a client
            Socket connectionSocket = welcomeSocket.accept();
            
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            DataInputStream inFromClient1 = new DataInputStream(connectionSocket.getInputStream());
            
           String clientRequest = inFromClient.readLine();
           String requestedFile = clientRequest.substring(2);
           
           //Checks if the received request is a RRQ or WRQ, if it's an RRQ :
            if(clientRequest.startsWith(Integer.toString(OP_RRQ))){
                            //Check if the requested file exists
                                if(isFile(requestedFile)){
                    System.out.println("Read request: " + requestedFile);
                    //sendFile(requestedFile, outToClient);
                    NikBerk(outToClient);
                    connectionSocket.close();
                    
                }
                                //Else a "file not found" error is raised, displayed and sent to the client
                                else{
                                    
                                    String fileNotFound = Integer.toString(OP_ERR) + " File not found";
                                    outToClient.writeBytes(fileNotFound);
                                    System.err.println(fileNotFound.substring(2));
                                    connectionSocket.close();
                                }
                                //Else if it's a WRQ : 
            }else if(clientRequest.startsWith(Integer.toString(OP_WRQ))){
                
                
                System.out.println("Write request: " + requestedFile);
                receiveFile(requestedFile, inFromClient1);
                connectionSocket.close();
            }else{
                System.err.print("Bad request");
                connectionSocket.close();
            }
            
            
            
        //    outToClient.writeBytes("zz");
            connectionSocket.close();
            
  }
    }
    
    
    /**
     * This method checks if the given file exists.
     * 
     * @param fileName 
     */
    private static boolean isFile(String fileName){
        String dir = READDIR + fileName;
        File file = new File(dir);
        if(file.isFile()){
            return true;
        }
        return false;
    }
    
    /**
     * This method sends the requested file to the client.
     * 
     * @param fileName 
     * @param outToClient
     */
    private static void sendFile(String fileName, DataOutputStream outToClient) throws IOException{
        
       // outToClient.writeUTF(fileName);  
        //outToClient.flush();  
        
        File f=new File(READDIR + fileName);
        FileInputStream fin=new FileInputStream(f);
        long sz=(int) f.length();
        
        byte b[]=new byte [BUFSIZE];

        int read;

     //   outToClient.writeUTF(Long.toString(sz)); 
       // outToClient.flush(); 

        System.out.println ("Size: "+sz);
      //  System.out.println ("Buf size: "+ss.getReceiveBufferSize());

      //Read the file and send it to client
        while((read = fin.read(b)) != -1){
           outToClient.write(b, 0, read); 
           outToClient.flush(); 
        }
        fin.close();
        
        System.out.println("..ok"); 
        outToClient.flush(); 
    }
    
    /**
     * This method receives the uploaded file from the client and saves it as "Server-fileName".
     * 
     * @param fileName 
     * @param inFromClient
     */
    private static void receiveFile(String fileName, DataInputStream inFromClient) throws IOException{
        String filename;
	
	filename="Server-"+fileName;
	System.out.println("Saving file as: "+filename);
        
        //Read and print the file size
//        long sz=Long.parseLong(inFromClient.readUTF());
  //      System.out.println ("File Size: "+(sz/(1024*1024))+" MB");

        //Receive the file from the client and write it into the writing path
        byte b[]=new byte [BUFSIZE];
        System.out.println("Receving file from client..");
        FileOutputStream fos=new FileOutputStream(new File(filename),true);
        long bytesRead;
        do
        {
        bytesRead = inFromClient.read(b, 0, b.length);
        fos.write(b,0,b.length);
        }while(!(bytesRead<1024));
        System.out.println("Comleted");
        fos.close(); 
    }
    
    private static void NikBerk(DataOutputStream outToClient) throws IOException{
        long sz = 222;
        String khra = "khraaaa\n";
       // outToClient.writeUTF(khra); 
        outToClient.writeBytes(khra);
        System.out.println(khra);
        outToClient.flush(); 
    }
}
