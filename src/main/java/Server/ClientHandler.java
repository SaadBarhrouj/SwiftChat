package Server;

import Dao.MessageDAO;
import Entities.User;
import Entities.Group;
import Dao.GroupDAO;
import Dao.UserDAO;
import Dao.DatabaseConnection;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
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
    private GroupDAO groupDAO;
    private MessageDAO messageDAO;
    private Connection conn;
    private Statement stmt;
    private ResultSet rs;

    public ClientHandler(Socket s, DataInputStream diss, DataOutputStream doss) {
        this.commthread = s;
        this.dis = diss;
        this.dos = doss;
        this.Auth = false;
        this.userDAO = new UserDAO();
        this.groupDAO = new GroupDAO();
        this.messageDAO = new MessageDAO();
        this.conn = DatabaseConnection.getConnection();
        if (this.conn != null) {
            try {
                this.stmt = this.conn.createStatement();
                this.start();
            } catch (SQLException e) {
                e.printStackTrace();
                error();
            }
        } else {
            error();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String choice = "";
                do {
                    this.showMenu();
                    choice = this.dis.readLine(); ;

                    String name, email, password;
                    switch (choice) {
                        case "a":
                            this.dos.writeUTF("Veuillez entrer votre nom :");
                            name = this.dis.readLine(); ;
                            this.dos.writeUTF("Veuillez entrer votre email :");
                            email = this.dis.readLine();
                            this.dos.writeUTF("Veuillez entrer votre mot de passe :");
                            password = this.dis.readLine(); ;
                            register(name, email, password);
                            break;
                        case "b":
                            this.dos.writeUTF("Veuillez entrer votre email :");
                            email = this.dis.readLine();
                            this.dos.writeUTF("Veuillez entrer votre mot de passe :");
                            password = this.dis.readLine(); ;
                            Auth = login(email, password);
                            break;
                        default:
                            this.dos.writeUTF("Choix invalide, veuillez réessayer");
                    }
                } while (!Auth);

                if (Auth) {

                    receiveAndDeleteMessages();
                    // receiveAndDeleteMessages();
                    userMenu();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            error();
        } finally {
            try {
                if (dis != null) dis.close();
                if (dos != null) dos.close();
                if (commthread != null && !commthread.isClosed()) commthread.close();
                if (conn != null && !conn.isClosed()) conn.close();
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void showMenu() throws IOException {
        this.dos.writeUTF("\n====== Menu Principal ======\n");
        this.dos.writeUTF("a. S'inscrire\n");
        this.dos.writeUTF("b. Se connecter\n");
        this.dos.writeUTF("============================\n");
        this.dos.writeUTF("Veuillez entrer votre choix :");
    }

    private void userMenu() throws IOException {
        String choice;
        do {
            this.dos.writeUTF("\n====== Menu Utilisateur ======\n");
            this.dos.writeUTF("a. Envoyer un message\n");
            this.dos.writeUTF("b. Envoyer un fichier\n");
            this.dos.writeUTF("c. Créer un groupe\n");
            this.dos.writeUTF("d. Rejoindre un groupe\n");
            this.dos.writeUTF("e. Mettre à jour le profil\n"); // Nouvelle option
            this.dos.writeUTF("f. Se déconnecter\n");
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
                    createGroup();
                    break;
                case "d":
                    joinGroup();
                    break;
                case "e": // Nouveau cas pour la mise à jour du profil
                    updateProfile();
                    break;
                case "f":
                    logout();
                    showMenu();
                    break;
                default:
                    this.dos.writeUTF("Choix invalide, veuillez réessayer");
            }
        } while (Auth);
    }

    private void createGroup() throws IOException {
        this.dos.writeUTF("Entrez le nom du groupe :");
        String name = this.dis.readLine();
        this.dos.writeUTF("Entrez la description du groupe :");
        String description = this.dis.readLine();

        if (groupDAO.createGroup(name, description, userAccount.getId())) {
            this.dos.writeUTF("Groupe créé avec succès.");
        } else {
            this.dos.writeUTF("Échec de la création du groupe.");
        }
    }

    private void joinGroup() throws IOException {
        this.dos.writeUTF("Entrez l'ID du groupe :");
        int groupId = Integer.parseInt(this.dis.readLine());

        Group group = groupDAO.getGroupById(groupId);
        if (group != null) {
            // Ajouter l'utilisateur au groupe dans la base de données
            boolean added = groupDAO.addUserToGroup(userAccount.getId(), groupId);
            if (added) {
                // Ajouter le groupe à la liste des groupes de l'utilisateur en mémoire
                userAccount.addGroup(group);
                this.dos.writeUTF("Vous avez rejoint le groupe " + group.getName());
            } else {
                this.dos.writeUTF("Erreur lors de l'ajout au groupe.");
            }
        } else {
            this.dos.writeUTF("Groupe introuvable.");
        }
    }



    private boolean login(String email, String password) {
        try {
            ResultSet rs = userDAO.getUserByEmailAndPassword(email, password);
            if (rs.next()) {
                dos.writeUTF("Connexion réussie");
                userAccount = new User(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"), rs.getString("password"));
                return true;
            }
            dos.writeUTF("Échec de la connexion");
            return false;
        } catch (SQLException | IOException e) {
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
        } catch (Exception e) {
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



    private void updateProfile() throws IOException {
        this.dos.writeUTF("Entrez votre nouveau nom :");
        String newName = this.dis.readLine();
        this.dos.writeUTF("Entrez votre nouvel email :");
        String newEmail = this.dis.readLine();
        this.dos.writeUTF("Entrez votre nouveau mot de passe :");
        String newPassword = this.dis.readLine();

        // Mettre à jour le profil de l'utilisateur
        userAccount.updateProfile(newEmail, newPassword, newName);

        this.dos.writeUTF("Profil mis à jour avec succès !");
    }

    private void logout() {
        try {
            dos.writeUTF("Déconnexion en cours...");
            Auth = false;
            if (userAccount != null) {
                userAccount.disconnect();
            }
            dis.close();
            dos.close();
            commthread.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void error() {
        if (this.Auth && this.userAccount != null) {
            this.userAccount.disconnect();
        }
        try {
            this.dis.close();
            this.dos.close();
        } catch (IOException e3) {
            e3.printStackTrace();
        }
        try {
            this.commthread.close();
        } catch (IOException e4) {
            e4.printStackTrace();
        }
    }
}