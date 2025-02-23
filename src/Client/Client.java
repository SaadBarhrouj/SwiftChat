package Client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Scanner scanner;

    public Client(String address, int port) {
        try {
            // Établir la connexion avec le serveur
            socket = new Socket(address, port);
            System.out.println("Connecté au serveur");

            // Initialiser les flux d'entrée et de sortie
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            scanner = new Scanner(System.in);

            // Lire les messages du serveur dans un thread séparé
            new Thread(this::listenForMessages).start();

            // Gérer les entrées utilisateur
            handleUserInput();
        } catch (IOException e) {
            System.err.println("Erreur lors de la connexion au serveur : " + e.getMessage());
        } finally {
            closeResources();
        }
    }

    private void listenForMessages() {
        try {
            while (true) {
                String message = dis.readUTF();
                System.out.println(message);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture des messages du serveur : " + e.getMessage());
        }
    }

    private void handleUserInput() {
        try {
            while (true) {
                String input = scanner.nextLine();
                dos.writeUTF(input);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi des données au serveur : " + e.getMessage());
        }
    }

    private void closeResources() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Erreur lors de la fermeture des ressources : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Client("localhost", 5059);
    }
}