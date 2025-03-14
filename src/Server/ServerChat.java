package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class ServerChat {

    public static void main(String[] args) {

        ServerSocket serverSocket = null; // Server socket to listen for incoming client connections

        try {

            serverSocket = new ServerSocket(5059);
            System.out.println("Server started . . .");
        } catch (Exception e) {

            System.out.println("Connection error: " + e.getMessage());
        }

        while (true) {
            Socket clientSocket = null;
            try {

                clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                InputStream inputStream = clientSocket.getInputStream();
                DataInputStream dataInputStream= new DataInputStream(inputStream);
                OutputStream outputStream = clientSocket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

                Thread clientHandlerThread = new ClientHandler(clientSocket, dataInputStream, dataOutputStream);

                clientHandlerThread.start();
            } catch (Exception e) {

                System.err.println("Error handling client connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}