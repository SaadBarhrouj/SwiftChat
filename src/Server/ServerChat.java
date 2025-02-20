package Server;

import Client.ClientHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * ServerChat class is responsible for setting up a server that listens for client connections
 * and handles communication with connected clients.
 */
public class ServerChat {
    public static int numClient;
    public static List <String> clientHandlersList=new ArrayList<>();
    /**
     * Main method to start the server and handle client connections.
     *
     * @param args Command-line arguments (not used in this implementation).
     */
    public static void main(String[] args) {
        clientHandlersList.add("saad");
        // ServerSocket for accepting client connections
        ServerSocket serverSocket = null;

        try {
            // Initialize the server socket on port 5059
            serverSocket = new ServerSocket(5059);
            System.out.println("Server started . . .");
        } catch (Exception e) {
            // Handle connection errors
            System.out.println("Connection error: " + e.getMessage());
        }

        // Initialize user profiles (assuming Profile.initializeProfiles() is defined elsewhere)
        // Profile.initializeProfiles();

        // Infinite loop to continuously accept client connections
        while (true) {
            Socket clientSocket = null;
            try {
                // Accept a new client connection
                clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                // Create input and output streams for communication with the client
                InputStream inputStream = clientSocket.getInputStream();
                DataInputStream dataInputStream= new DataInputStream(inputStream);
                OutputStream outputStream = clientSocket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

                // Create a new thread to handle the client
                Thread clientHandlerThread = new ClientHandler(clientSocket, dataInputStream, dataOutputStream);

                ++numClient;
                clientHandlerThread.start(); // Start the thread (assuming ClientHandler implements Runnable)
            } catch (Exception e) {
                // Print stack trace for any exceptions during client handling
                System.err.println("Error handling client connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}