package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The ServerChat class is responsible for setting up a server that listens for client connections
 * and manages communication with connected clients.
 */
public class ServerChat {

    /**
     * Main method to start the server and handle client connections.
     *
     * @param args Command-line arguments (not used in this implementation).
     */
    public static void main(String[] args) {

        ServerSocket serverSocket = null; // Server socket to listen for incoming client connections

        try {
            /**
             * Initializes the server socket on port 5059 and starts listening for connections.
             * Prints a message indicating that the server has started.
             */
            serverSocket = new ServerSocket(5059);
            System.out.println("Server started . . .");
        } catch (Exception e) {
            /**
             * Handles exceptions that may occur while starting the server.
             * Prints an error message in case of failure.
             */
            System.out.println("Connection error: " + e.getMessage());
        }

        /**
         * Infinite loop to continuously accept client connections and create a new thread for each client.
         */
        while (true) {
            Socket clientSocket = null;
            try {
                /**
                 * Accepts an incoming client connection and prints the client's information.
                 */
                clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                /**
                 * Creates input and output streams for communication with the client.
                 */
                InputStream inputStream = clientSocket.getInputStream();
                DataInputStream dataInputStream= new DataInputStream(inputStream);
                OutputStream outputStream = clientSocket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

                /**
                 * Creates a new thread to handle communication with the connected client.
                 */
                Thread clientHandlerThread = new ClientHandler(clientSocket, dataInputStream, dataOutputStream);

                /**
                 * Starts the client handler thread to process client requests.
                 */
                clientHandlerThread.start();
            } catch (Exception e) {
                /**
                 * Handles errors that may occur while accepting or communicating with clients.
                 * Prints an error message and stack trace.
                 */
                System.err.println("Error handling client connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}