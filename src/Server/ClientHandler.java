package Server;

import Dao.ContactDAO;
import Dao.MessageDAO;
import Entities.Contact;
import Entities.Message;
import Entities.User;
import Dao.UserDAO;
import Dao.GroupDAO;
import Dao.DatabaseConnection;
import Entities.Group;

import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La classe ClientHandler gère la communication avec un client connecté au serveur.
 * Elle permet la gestion de l'authentification (inscription et connexion), ainsi que la gestion des contacts, messages et groupes.
 */
public class ClientHandler extends Thread {
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final Socket commthread;
    private boolean Auth;
    private User userAccount;
    private UserDAO userDAO;
    private ContactDAO contactDAO;
    private MessageDAO messageDAO;
    private GroupDAO groupDAO;
    private Connection conn;
    private Statement stmt;
    private static Map<String, DataOutputStream> mapDos = new HashMap<>();
    /**
     * Constructeur de la classe ClientHandler.
     *
     * @param s Socket du client.
     * @param diss Flux d'entrée des données du client.
     * @param doss Flux de sortie des données vers le client.
     */
    public ClientHandler(Socket s, DataInputStream diss, DataOutputStream doss) {
        this.commthread = s;
        this.dis = diss;
        this.dos = doss;
        this.Auth = false;
        this.userDAO = new UserDAO();
        this.contactDAO = new ContactDAO();
        this.messageDAO = new MessageDAO();
        this.groupDAO = new GroupDAO();
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

    /**
     * Méthode principale qui gère la boucle d'attente de l'authentification et le menu utilisateur.
     */
    @Override
    public void run() {
        try {
            while (true) {
                authenticateUser();
                if (Auth) {
                    receiveAndDeleteMessages();
                    userMenu();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            error();
        } finally {
            cleanup();
        }
    }

    private void authenticateUser() throws IOException {
        String choice = "";
        do {
            showMainMenu();
            choice = this.dis.readLine();

            String name, email, password, confirmPassword;
            switch (choice) {
                case "a":
                    // Inscription
                    this.dos.writeUTF("Veuillez entrer votre nom :");
                    name = this.dis.readLine();
                    this.dos.writeUTF("Veuillez entrer votre email :");
                    email = this.dis.readLine();
                    this.dos.writeUTF("Veuillez entrer votre mot de passe :");
                    password = this.dis.readLine();
                    this.dos.writeUTF("Veuillez confirmer votre mot de passe :");
                    confirmPassword = this.dis.readLine();
                    Auth = register(name, email, password, confirmPassword);
                    break;
                case "b":
                    // Connexion
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
    }

    private void showMainMenu() throws IOException {
        this.dos.writeUTF("\n====== Menu Principal ======\n");
        this.dos.writeUTF("a. S'inscrire\n");
        this.dos.writeUTF("b. Se connecter\n");
        this.dos.writeUTF("============================\n");
        this.dos.writeUTF("Veuillez entrer votre choix :");
    }


    /**
     * Méthode pour gérer la connexion d'un utilisateur.
     * Vérifie si l'email et le mot de passe ne sont pas vides avant de procéder à la connexion.
     *
     * @param email L'email de l'utilisateur.
     * @param password Le mot de passe de l'utilisateur.
     * @return true si la connexion est réussie, sinon false.
     */
    private boolean login(String email, String password) {
        try {
            // Vérification des informations de connexion
            ResultSet rs = userDAO.getUserByEmailAndPassword(email, password);
            if (rs.next()) {
                dos.writeUTF("Connexion réussie");
                userAccount = new User(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"), rs.getString("password"));
                userDAO.setUserOnlineStatus(userAccount.getId(), true);

                // Ajouter l'utilisateur à la Map
                mapDos.put(email, dos);

                // Afficher les messages en attente
                receiveAndDeleteMessages();

                return true;
            }
            dos.writeUTF("Échec de la connexion");
            return false;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Méthode pour gérer l'inscription d'un utilisateur.
     * Vérifie que l'email, le mot de passe et la confirmation du mot de passe sont valides.
     *
     * @param name Le nom de l'utilisateur.
     * @param email L'email de l'utilisateur.
     * @param password Le mot de passe de l'utilisateur.
     * @param confirmPassword La confirmation du mot de passe.
     * @return true si l'inscription est réussie, sinon false.
     */
    private boolean register(String name, String email, String password, String confirmPassword) {
        try {
            // Vérification des champs vides
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                dos.writeUTF("Tous les champs doivent être remplis.");
                return false;
            }

            // Validation de l'email
            if (!isValidEmail(email)) {
                dos.writeUTF("Format d'email invalide.");
                return false;
            }

            // Validation du mot de passe
            if (!isValidPassword(password)) {
                dos.writeUTF("Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule et un chiffre.");
                return false;
            }

            // Vérification de la correspondance des mots de passe
            if (!password.equals(confirmPassword)) {
                dos.writeUTF("Les mots de passe ne correspondent pas.");
                return false;
            }

            if (userDAO.userExists(email)) {
                dos.writeUTF("Email déjà utilisé");
                return false;
            }

            if (userDAO.insertUser(name, email, password, confirmPassword)) {
                ResultSet rs = userDAO.getUserByEmailAndPassword(email, password);
                if (rs.next()) {
                    userAccount = new User(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"), rs.getString("password"));
                    dos.writeUTF("Inscription réussie");
                    userDAO.setUserOnlineStatus(userAccount.getId(), true); // Mettre à jour le statut en ligne
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Vérifie si l'email est valide.
     *
     * @param email L'email à vérifier.
     * @return true si l'email est valide, sinon false.
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * Vérifie si le mot de passe est valide.
     *
     * @param password Le mot de passe à vérifier.
     * @return true si le mot de passe est valide, sinon false.
     */
    private boolean isValidPassword(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*[0-9].*");
    }

    private void userMenu() throws IOException {
        String choice;
        do {
            dos.writeUTF("\n====== Menu Utilisateur ======\n");
            dos.writeUTF("1. Gestion des Contacts\n");
            dos.writeUTF("2. Gestion des Messages\n");
            dos.writeUTF("3. Gestion des Groupes\n");
            dos.writeUTF("4. Mettre à jour le profil\n");
            dos.writeUTF("5. Se déconnecter\n");
            dos.writeUTF("==============================\n");
            dos.writeUTF("Veuillez entrer votre choix :");

            choice = dis.readLine();
            switch (choice) {
                case "1":
                    contactMenu();
                    break;
                case "2":
                    messageMenu();
                    break;
                case "3":
                    groupMenu();
                    break;
                case "4":
                    updateProfile();
                    break;
                case "5":
                    logout();
                    break;
                default:
                    dos.writeUTF("Choix invalide, veuillez réessayer");
            }
        } while (!choice.equals("5"));
    }

    private void contactMenu() throws IOException {
        String choice;
        do {
            dos.writeUTF("\n====== Menu Contacts ======\n");
            dos.writeUTF("a. Ajouter un contact\n");
            dos.writeUTF("b. Supprimer un contact\n");
            dos.writeUTF("c. Modifier le surnom\n");
            dos.writeUTF("d. Lister les contacts\n");
            dos.writeUTF("e. Retour au menu principal\n");
            dos.writeUTF("==============================\n");
            dos.writeUTF("Veuillez entrer votre choix :");

            choice = dis.readLine();
            switch (choice) {
                case "a":
                    handleAddContact();
                    break;
                case "b":
                    handleDeleteContact();
                    break;
                case "c":
                    handleUpdateNickname();
                    break;
                case "d":
                    handleListContacts();
                    break;
                case "e":
                    break; // Retour au menu principal
                default:
                    dos.writeUTF("Choix invalide, veuillez réessayer");
            }
        } while (!choice.equals("e"));
    }

    private void messageMenu() throws IOException {
        String choice;
        do {
            dos.writeUTF("\n====== Menu Messages ======\n");
            dos.writeUTF("a. Envoyer un message\n");
            dos.writeUTF("b. Recevoir des messages\n");
            dos.writeUTF("c. Retour au menu principal\n");
            dos.writeUTF("==============================\n");
            dos.writeUTF("Veuillez entrer votre choix :");

            choice = dis.readLine();
            switch (choice) {
                case "a":
                    sendUserMessage();
                    break;
                case "b":
                    displayConversation();                    break;
                case "c":
                    break; // Retour au menu principal
                default:
                    dos.writeUTF("Choix invalide, veuillez réessayer");
            }
        } while (!choice.equals("c"));
    }

    private void groupMenu() throws IOException {
        String choice;
        do {
            dos.writeUTF("\n====== Menu Groupes ======\n");
            dos.writeUTF("a. Créer un groupe\n");
            dos.writeUTF("b. Rejoindre un groupe\n");
            dos.writeUTF("c. Retour au menu principal\n");
            dos.writeUTF("==============================\n");
            dos.writeUTF("Veuillez entrer votre choix :");

            choice = dis.readLine();
            switch (choice) {
                case "a":
                    createGroup();
                    break;
                case "b":
                    joinGroup();
                    break;
                case "c":
                    break; // Retour au menu principal
                default:
                    dos.writeUTF("Choix invalide, veuillez réessayer");
            }
        } while (!choice.equals("c"));
    }

    private void displayConversation() throws IOException {
        dos.writeUTF("Entrez l'email de l'utilisateur avec qui vous souhaitez voir la conversation :");
        String email = dis.readLine();
        int contactUserId = userDAO.getUserIdByEmail(email);
        if (contactUserId == -1) {
            dos.writeUTF("Utilisateur introuvable !");
            return;
        }

        List<Message> messages = messageDAO.getConversation(userAccount.getId(), contactUserId);
        if (messages.isEmpty()) {
            dos.writeUTF("Aucun message trouvé.");
        } else {
            for (Message msg : messages) {
                dos.writeUTF("De " + msg.getSenderEmail() + " à " + msg.getDate() + " : " + msg.getMessage());
            }
        }
    }

    private void handleAddContact() throws IOException {
        dos.writeUTF("Entrez l'email du contact :");
        String email = dis.readLine();
        int contactUserId = userDAO.getUserIdByEmail(email);
        if (contactUserId == -1) {
            dos.writeUTF("Utilisateur introuvable !");
            return;
        }
        dos.writeUTF("Entrez un surnom (optionnel) :");
        String nickname = dis.readLine();
        boolean success = contactDAO.addContact(userAccount.getId(), contactUserId, nickname);
        dos.writeUTF(success ? "Contact ajouté !" : "Échec de l'ajout");
    }

    private void handleDeleteContact() throws IOException {
        dos.writeUTF("Entrez l'email du contact à supprimer :");
        String email = dis.readLine();
        int contactUserId = userDAO.getUserIdByEmail(email);
        if (contactUserId == -1) {
            dos.writeUTF("Aucun utilisateur trouvé avec cet email");
            return;
        }
        boolean success = contactDAO.deleteContact(userAccount.getId(), contactUserId);
        dos.writeUTF(success ? "Contact supprimé avec succès" : "Erreur lors de la suppression");
    }

    private void handleUpdateNickname() throws IOException {
        dos.writeUTF("Entrez l'email du contact :");
        String email = dis.readLine();
        int contactUserId = userDAO.getUserIdByEmail(email);
        if (contactUserId == -1) {
            dos.writeUTF("Contact introuvable");
            return;
        }
        dos.writeUTF("Entrez le nouveau surnom :");
        String newNickname = dis.readLine();
        boolean success = contactDAO.updateNickname(userAccount.getId(), contactUserId, newNickname);
        dos.writeUTF(success ? "Surnom mis à jour" : "Échec de la mise à jour");
    }

    private void handleListContacts() throws IOException {
        List<Contact> contacts = contactDAO.getContacts(userAccount.getId());
        if (contacts.isEmpty()) {
            dos.writeUTF("📭 Aucun contact trouvé");
            return;
        }
        StringBuilder sb = new StringBuilder("Liste des contacts :\n");
        for (Contact contact : contacts) {
            String email = userDAO.getEmailById(contact.getContactUserId());
            sb.append("➤ ").append(email)
                    .append(contact.getNickname() != null ? " (" + contact.getNickname() + ")" : "")
                    .append("\n");
        }
        dos.writeUTF(sb.toString());
    }



    public class SoundPlayer {
        public static void playSound(String soundFilePath) {
            try {
                // Charger le fichier audio
                File soundFile = new File(soundFilePath);
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);

                // Obtenir un Clip pour jouer le son
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);

                // Jouer le son
                clip.start();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
            }
        }
    }



    private void sendUserMessage() throws IOException {
        // 1. Demander l'email du destinataire
        this.dos.writeUTF("Entrez l'email du destinataire :");
        String recipient = this.dis.readLine();

        // 2. Vérifier si le destinataire existe
        int recipientUserId = userDAO.getUserIdByEmail(recipient);
        if (recipientUserId == -1) {
            this.dos.writeUTF("Utilisateur introuvable !");
            return;
        }

        // 3. Vérifier si le destinataire est un contact
        if (!contactDAO.areContacts(userAccount.getId(), recipientUserId)) {
            this.dos.writeUTF("Le destinataire n'est pas dans votre liste de contacts.");
            this.dos.writeUTF("Souhaitez-vous ajouter ce contact ? (oui/non)");
            String response = this.dis.readLine();
            if (response.equalsIgnoreCase("oui")) {
                handleAddContact(); // Rediriger vers la méthode d'ajout de contact
                return; // Retourner à l'appelant après la gestion des contacts
            } else {
                this.dos.writeUTF("Retour au menu principal.");
                return; // Retourner au menu principal
            }
        }

        // 4. Demander le message à envoyer
        this.dos.writeUTF("Entrez votre message :");
        String message = this.dis.readLine();

        // 5. Vérifier si le destinataire est en ligne
        if (mapDos.containsKey(recipient)) {
            // Envoyer le message en temps réel
            DataOutputStream recipientDos = mapDos.get(recipient);
            try {
                recipientDos.writeUTF("Nouveau message de " + userAccount.getEmail() + " : " + message);
                this.dos.writeUTF("Message envoyé avec succès.");

                // Jouer un son pour le destinataire
                SoundPlayer.playSound("C:\\Users\\Lenovo\\IdeaProjects\\ChatApplication\\src\\utils\\notif.wav"); // Chemin relatif
            } catch (IOException e) {
                e.printStackTrace();
                this.dos.writeUTF("Échec de l'envoi du message.");
            }
        } else {
            // Stocker le message en attente
            boolean stored = messageDAO.storePendingMessage(userAccount.getId(), recipientUserId, message);
            if (stored) {
                this.dos.writeUTF("Le destinataire est déconnecté. Le message sera délivré à sa reconnexion.");
            } else {
                this.dos.writeUTF("Échec de l'enregistrement du message en attente.");
            }
        }
    }


    private void receiveAndDeleteMessages() {
        try {
            var messages = messageDAO.getPendingMessagesForUser(userAccount.getId());
            if (messages.isEmpty()) {
                dos.writeUTF("Aucun nouveau message.");
            } else {
                for (var msg : messages) {
                    dos.writeUTF("De " + msg.getSenderEmail() + " à " + msg.getDate() + " : " + msg.getMessage());
                }
                messageDAO.deletePendingMessagesForUser(userAccount.getId());
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la réception ou de la suppression des messages : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createGroup() throws IOException {
        dos.writeUTF("Entrez le nom du groupe :");
        String name = dis.readLine();
        dos.writeUTF("Entrez la description du groupe :");
        String description = dis.readLine();
        if (groupDAO.createGroup(name, description, userAccount.getId())) {
            dos.writeUTF("Groupe créé avec succès.");
        } else {
            dos.writeUTF("Échec de la création du groupe.");
        }
    }

    private void joinGroup() throws IOException {
        dos.writeUTF("Entrez l'ID du groupe :");
        int groupId = Integer.parseInt(dis.readLine());
        Group group = groupDAO.getGroupById(groupId);
        if (group != null) {
            boolean added = groupDAO.addUserToGroup(userAccount.getId(), groupId);
            if (added) {
                userAccount.addGroup(group);
                dos.writeUTF("Vous avez rejoint le groupe " + group.getName());
            } else {
                dos.writeUTF("Erreur lors de l'ajout au groupe.");
            }
        } else {
            dos.writeUTF("Groupe introuvable.");
        }
    }

    private void updateProfile() throws IOException {
        dos.writeUTF("Entrez votre nouveau nom :");
        String newName = dis.readLine();
        dos.writeUTF("Entrez votre nouvel email :");
        String newEmail = dis.readLine();
        dos.writeUTF("Entrez votre nouveau mot de passe :");
        String newPassword = dis.readLine();
        userAccount.updateProfile(newEmail, newPassword, newName);
        dos.writeUTF("Profil mis à jour avec succès !");
    }




    private void logout() {
        try {
            dos.writeUTF("Déconnexion en cours...");
            Auth = false;
            if (userAccount != null) {
                mapDos.remove(userAccount.getEmail()); // Retirer l'utilisateur de la Map
                userDAO.setUserOnlineStatus(userAccount.getId(), false);
            }
            dis.close();
            dos.close();
            commthread.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cleanup() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (commthread != null && !commthread.isClosed()) commthread.close();
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (IOException | SQLException e) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            this.commthread.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
