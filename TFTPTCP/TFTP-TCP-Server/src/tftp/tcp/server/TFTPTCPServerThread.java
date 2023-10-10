/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tftp.tcp.server;
import java.net.*;
import java.io.*;
/**
 *
 * @author ab811
 */
public class TFTPTCPServerThread extends Thread{
    private Socket slaveSocket = null;
    
    public TFTPTCPServerThread(Socket socket){
        super("TFTPTCPServerThread");
        this.slaveSocket = socket;
    }
    
    @Override
    public void run(){
        String line;
        
        PrintWriter socketOutput;
        BufferedReader socketInput;
        
        try {
            socketOutput = new PrintWriter(slaveSocket.getOutputStream(), true);
            socketInput = new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()));
            while ((line = socketInput.readLine()) != null) {
                System.out.println("Echoing: " + line + ", which is " + line.length() +  " characters long..");
                socketOutput.println(line);
            }
            
            System.err.println("Closing Socket");
            slaveSocket.close();
            
        } catch (IOException e) {
            System.err.println(e);
        }
        }
    }
    

