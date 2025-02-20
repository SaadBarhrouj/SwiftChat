package Client;
import Server.ServerChat;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;


/**
 * ClientHandler class handles communication between the server and a connected client.
 * It extends the Thread class to enable concurrent handling of multiple clients.
 * Each client is assigned a separate thread to allow simultaneous interactions.
 */
public class ClientHandler extends Thread {
    private final DataInputStream dis; // Input stream to receive data from the client
    private final DataOutputStream dos; // Output stream to send data to the client
    private final Socket commthread; // Socket for client communication
    private Connection conn; // Database connection
    private Statement stmt; // SQL statement execution
    private ResultSet rs; // Result set for database queries
    private String msg; // Message received from the client
    private String receiver; // Receiver of the message

    /**
     * Constructor to initialize the client handler with the client's socket and streams.
     *
     * @param s    Client socket for communication
     * @param diss Data input stream to receive data
     * @param doss Data output stream to send data
     */
    public ClientHandler(Socket s, DataInputStream diss, DataOutputStream doss) {
        this.commthread = s;
        this.dis = diss;
        this.dos = doss;
    }

    /**
     * Overrides the run method to handle client communication in a separate thread.
     * The method sends a welcome message and logs the client's IP address.
     */
    @Override
    public void run() {
        try {
           int i=0;
           boolean auth=false;
           while(i<=2){
               this.dos.writeUTF("entrer votre mail\n : ");
               String email=this.dis.readLine();
               for (String e :ServerChat.clientHandlersList){
                   if (e.equals(email)){
                       System.out.println("Connexion du client numéro IP: " + commthread.getInetAddress());
                       this.dos.writeUTF("Welcome to the chat server! Your number is : " + ServerChat.numClient);
                       auth= true;
                       break;
                   }
               }
               if (auth){
                   break;
               }
               i++;
           }

           if (!auth){
               this.dos.writeUTF("noAuth");
           }else{
              while(true){
                  this.dis.readLine();
                  this.dos.writeUTF("hiii");
              }
           }





        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}