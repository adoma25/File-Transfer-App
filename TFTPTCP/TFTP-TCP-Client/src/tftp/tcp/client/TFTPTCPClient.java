/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tftp.tcp.client;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author ab811
 */
public class TFTPTCPClient {

   /**
    * TFTP Client port and Buf size.
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
     */
    public static void main(String[] args) throws IOException {

        while (true) {

            Socket clientSocket = new Socket("localhost", TFTPTCPPORT);
            DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

            int selection = menu();
            switch (selection) {
                case 1:
                    {
                        System.out.println("Enter file name to download");
                        Scanner name = new Scanner(System.in);
                        String fileName = name.next();
                        String request = Integer.toString(OP_RRQ) + "," + fileName + '\n';
                        outToServer.writeBytes(request);
                        String reply = inFromServer.readLine();
                        if(reply.startsWith(Integer.toString(OP_ERR))){
                            System.err.println(reply.substring(2));
                            clientSocket.close();
                        }else{
                            //downloadFile(fileName, inFromServer);
                            nikBerk(inFromServer);
                            clientSocket.close();
                        }   break;
                    }
                case 2:
                    {
                        System.out.println("Enter file name to upload");
                        Scanner name = new Scanner(System.in);
                        String fileName = name.next();
                        if (isFile(fileName)) {
                            String request = Integer.toString(OP_WRQ) + "," + fileName + '\n';
                            outToServer.writeBytes(request);
                            
                            uploadFile(fileName, outToServer);
                            clientSocket.close();
                        } else {
                            System.err.print("Requested file not found");
                        }       break;
                    }
                case 3:
                    System.exit(0);
                    
                default:
                    break;
            }

        }
    }

    
    /**
     * This method generates a menu with 3 options: Download, Upload or Exit.
     * Returns the user selection.
     * 
     * @return selection 
     * 
     */
    private static int menu() {
        int selection;
        Scanner input = new Scanner(System.in);

        /**
         * *************************************
         */
        System.out.println("/****************************/");
        System.out.println("/*    Connected to server   */");
        System.out.println("/*Enter 1 to download a file*/");
        System.out.println("/*Enter 2 to upload a file* */");
        System.out.println("/*Enter 3 to exit           */");
        System.out.println("/****************************/");

        selection = input.nextInt();
        return selection;
    }

    /**
     * This method receives the requested file from the server and writes it in the client's workspace destination.
     * 
     * @param fileName 
     * @param inFromServer
     */
    private static void downloadFile(String fileName, DataInputStream inFromServer) throws IOException {
        String filename = fileName;//inFromServer.readUTF(); 
        System.out.println("Receving file: " + filename);
        filename = "client" + filename;
        System.out.println("Saving as file: " + filename);
        //
//          long sz=Long.parseLong(inFromServer.readUTF());
  //         System.out.println ("File Size: "+(sz/(1024*1024))+" MB");

        byte b[] = new byte[BUFSIZE];
        System.out.println("Receving file..");
        FileOutputStream fos = new FileOutputStream(new File(filename), true);
        long bytesRead;
        do {
            bytesRead = inFromServer.read(b, 0, b.length);
            fos.write(b, 0, b.length);
        } while (!(bytesRead < 1024));
        System.out.println("Comleted");
        fos.close();
        

    }

    /**
     * This method sends the name of the desired file to upload and then sends the file data.
     * 
     * @param fileName 
     * @param outToServer
     */
    private static void uploadFile(String fileName, DataOutputStream outToServer) throws IOException {
        outToServer.writeUTF(fileName);
        outToServer.flush();

        File file = new File(READDIR + fileName);
        FileInputStream fileInStream = new FileInputStream(file);
        long sz = (int) file.length();

        byte b[] = new byte[BUFSIZE];

        int read;

        //outToServer.writeUTF(Long.toString(sz));
        //outToServer.flush();

        System.out.println("Size: " + sz + "bytes");

        while ((read = fileInStream.read(b)) != -1) {
            outToServer.write(b, 0, read);
            outToServer.flush();
            
        }
        fileInStream.close();

        System.out.println("..ok");
        outToServer.flush();

    }

    /**
     * This method checks if the given file exists.
     * 
     * @param fileName 
     */
    private static boolean isFile(String fileName) {
        String dir = READDIR + fileName;
        File file = new File(dir);
        if (file.isFile()) {
            return true;
        }
        return false;
    }
    
    private static void nikBerk(DataInputStream inFromServer) throws IOException{
      //  long sz=Long.parseLong(inFromServer.readUTF());
      String khra = inFromServer.readLine();
      System.out.println ("File Size: "+(khra)+" MB");
    }

}
