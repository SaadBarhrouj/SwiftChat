package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * ServerChat est la classe principale qui configure et démarre le serveur Socket.
 * Le serveur écoute sur le port 5059 et accepte les connexions entrantes des clients.
 * Pour chaque client connecté, un nouveau thread est créé pour gérer la communication.
 */
public class ServerChat {

    public static void main(String[] args) {
        ServerSocket serverSocket = null; // Server socket pour écouter les connexions entrantes des clients

        try {
            // Création du serveur Socket sur le port 5059
            serverSocket = new ServerSocket(5059);
            System.out.println("Server started . . .");

            // Boucle infinie pour accepter les connexions des clients
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                // Création des flux d'entrée et de sortie pour communiquer avec le client
                DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

                // Création d'un nouveau thread pour gérer la communication avec le client
                Thread clientHandlerThread = new ClientHandler(clientSocket, dataInputStream, dataOutputStream);
                clientHandlerThread.start();
            }
        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (Exception e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
    }
}