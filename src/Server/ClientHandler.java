package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * ClientHandler est une classe qui gère la communication avec un client connecté.
 * Chaque instance de cette classe est exécutée dans un thread séparé pour permettre
 * une communication simultanée avec plusieurs clients.
 */
public class ClientHandler extends Thread {
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final Socket clientSocket;

    /**
     * Constructeur de la classe ClientHandler.
     *
     * @param s   Le socket du client connecté.
     * @param dis Le flux d'entrée pour lire les données du client.
     * @param dos Le flux de sortie pour envoyer des données au client.
     */
    public ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos) {
        this.clientSocket = s;
        this.dis = dis;
        this.dos = dos;
        this.start(); // Démarre le thread dès que l'objet est créé
    }

    @Override
    public void run() {
        try {
            // Boucle pour gérer la communication avec le client
            while (true) {
                // Exemple de lecture d'un message du client
                String message = dis.readUTF();
                System.out.println("Message from client: " + message);

                // Exemple d'envoi d'un message au client
                dos.writeUTF("Message received: " + message);
            }
        } catch (IOException e) {
            System.err.println("Error handling client connection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                // Fermeture des flux et du socket
                if (dis != null) dis.close();
                if (dos != null) dos.close();
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}
