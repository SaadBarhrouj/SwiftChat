package Server;

import DAO.User;
import Database.MessageDAO;
import Database.UserDAO;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClientHandler extends Thread {
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final Socket commthread;
    private boolean Auth;
    private User userAccount;
    private UserDAO userDAO;
    private MessageDAO messageDAO;

    public ClientHandler(Socket s, DataInputStream diss, DataOutputStream doss) {
        this.commthread = s;
        this.dis = diss;
        this.dos = doss;
        this.Auth = false;
        this.userDAO = new UserDAO();
        this.messageDAO = new MessageDAO();
    }

    @Override
    public void run() {
        try {
            while (true) {
                String choice = "";
                do {
                    this.showMenu();
                    choice = this.dis.readLine();  // Lecture du choix du client

                    String name, email, password;
                    switch (choice) {
                        case "a":  // Cas d'inscription
                            this.dos.writeUTF("Veuillez entrer votre nom :");
                            name = this.dis.readLine();
                            this.dos.writeUTF("Veuillez entrer votre email :");
                            email = this.dis.readLine();
                            this.dos.writeUTF("Veuillez entrer votre mot de passe :");
                            password = this.dis.readLine();
                            register(name, email, password);
                            break;
                        case "b":  // Cas de connexion
                            this.dos.writeUTF("Veuillez entrer votre email :");
                            email = this.dis.readLine();
                            this.dos.writeUTF("Veuillez entrer votre mot de passe :");
                            password = this.dis.readLine();
                            Auth = login(email, password);
                            break;
                        default:
                            this.dos.writeUTF("Choix invalide, veuillez réessayer");
                    }
                } while (!Auth);

                if (Auth) {
                    // Afficher les messages en attente dès la connexion
                    receiveAndDeleteMessages();
                    userMenu();
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur dans ClientHandler : " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (dis != null) dis.close();
                if (dos != null) dos.close();
                if (commthread != null && !commthread.isClosed()) commthread.close();
            } catch (IOException e) {
                System.err.println("Erreur lors de la fermeture des ressources : " + e.getMessage());
            }
        }
    }

    private void userMenu() throws IOException {
        String choice;
        do {
            this.dos.writeUTF("\n====== Menu Utilisateur ======\n");
            this.dos.writeUTF("a. Envoyer un message\n");
            this.dos.writeUTF("b. Envoyer un fichier\n");
            this.dos.writeUTF("c. Se déconnecter\n");
            this.dos.writeUTF("==============================\n");
            this.dos.writeUTF("Veuillez entrer votre choix :");

            choice = this.dis.readLine();
            switch (choice) {
                case "a":
                    sendUserMessage();
                    break;
                case "b":
                    sendUserFile();
                    break;
                case "c":
                    userAccount.disconnect();
                    this.dos.writeUTF("Déconnexion en cours...");
                    Auth = false;
                    break;
                default:
                    this.dos.writeUTF("Choix invalide, veuillez réessayer");
            }
        } while (Auth);
    }

    private boolean login(String email, String password) {
        try {
            var rs = userDAO.getUserByEmailAndPassword(email, password);
            if (rs.next()) {
                dos.writeUTF("Connexion réussie");
                userAccount = new User(rs.getInt("user_id"), rs.getString("email"), rs.getString("name"), dos, dis);
                return true;
            }
            dos.writeUTF("Échec de la connexion");
            return false;
        } catch (SQLException | IOException e) {
            System.err.println("Erreur lors de la connexion : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean register(String name, String email, String password) {
        try {
            if (userDAO.userExists(email)) {
                dos.writeUTF("Email déjà utilisé");
                return false;
            } else {
                if (userDAO.insertUser(name, email, password)) {
                    dos.writeUTF("Inscription réussie");
                    return true;
                }
            }
        } catch (SQLException | IOException e) {
            System.err.println("Erreur lors de l'inscription : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private void sendUserMessage() throws IOException {
        this.dos.writeUTF("Entrez l'email du destinataire :");
        String recipient = this.dis.readLine();
        this.dos.writeUTF("Entrez votre message :");
        String message = this.dis.readLine();

        boolean sent = userAccount.sendMessage(recipient, message);
        if (sent) {
            this.dos.writeUTF("Message envoyé avec succès.");
        } else {
            this.dos.writeUTF("Échec de l'envoi du message.");
        }
    }

    private void sendUserFile() throws IOException {
        this.dos.writeUTF("Entrez l'email du destinataire :");
        String recipient = this.dis.readLine();
        this.dos.writeUTF("Entrez le chemin complet du fichier :");
        String filePath = this.dis.readLine();

        try {
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            String fileName = Paths.get(filePath).getFileName().toString();

            boolean sent = userAccount.sendFile(recipient, fileName, fileData);
            if (sent) {
                this.dos.writeUTF("Fichier envoyé avec succès.");
            } else {
                this.dos.writeUTF("Échec de l'envoi du fichier.");
            }
        } catch (IOException e) {
            this.dos.writeUTF("Erreur lors de la lecture du fichier : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void receiveAndDeleteMessages() {
        try {
            var messages = messageDAO.getMessagesForUser(userAccount.getEmail());
            if (messages.isEmpty()) {
                dos.writeUTF("Aucun nouveau message.");
            } else {
                for (var msg : messages) {
                    dos.writeUTF("De " + msg.getSenderEmail() + " à " + msg.getDate() + " : " + msg.getMessage());
                }
                messageDAO.deleteMessagesForUser(userAccount.getEmail());
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la réception ou de la suppression des messages : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showMenu() throws IOException {
        try {
            this.dos.writeUTF("\n====== Menu Principal ======\n");
            this.dos.writeUTF("\na. Créer un compte");
            this.dos.writeUTF("\nb. Se connecter");
            this.dos.writeUTF("\nc. Quitter");
            this.dos.writeUTF("\n============================");
            this.dos.writeUTF("\nVeuillez entrer votre choix :");
        } catch (IOException e) {
            System.err.println("Erreur lors de l'affichage du menu : " + e.getMessage());
            throw e;  // Relancer pour être capturé dans la méthode appelante
        }
    }
}