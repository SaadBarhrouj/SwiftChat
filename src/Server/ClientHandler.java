package Server;

import Dao.*;
import Entities.*;
import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import utils.SoundPlayer;

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

    @Override
    public void run() {
        try {
            authenticateUser();
            if (Auth) {
                receiveAndDeleteMessages(); // Afficher et supprimer les messages en attente
                while (true) {
                    userMenu();
                    String messageType = dis.readUTF();
                    switch (messageType) {
                        case "text":
                            // Handle text message
                            sendUserMessage();
                            break;
                        case "file":
                            // Handle file message
                            receiveFile();
                            break;
                        default:
                            dos.writeUTF("Type de message inconnu.");
                            break;
                    }
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
        String choice;
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

    private void receiveFile() throws IOException {
        new Thread(() -> {
            try {
                // 1. Recevoir le nom du fichier et sa taille
                String fileName = dis.readUTF();
                long fileSize = dis.readLong();
    
                // 2. Créer un fichier local pour stocker les données reçues
                File file = new File("received_" + fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    // Recevoir le fichier par morceaux
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalBytesRead = 0;
                    while (totalBytesRead < fileSize) {
                        bytesRead = dis.read(buffer, 0, buffer.length);
                        totalBytesRead += bytesRead;
                        fos.write(buffer, 0, bytesRead);
                    }
    
                    dos.writeUTF("Fichier reçu avec succès : " + file.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                    dos.writeUTF("Échec de la réception du fichier.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showMainMenu() throws IOException {
        this.dos.writeUTF("\n====== Menu Principal ======\n");
        this.dos.writeUTF("a. S'inscrire\n");
        this.dos.writeUTF("b. Se connecter\n");
        this.dos.writeUTF("============================\n");
        this.dos.writeUTF("Veuillez entrer votre choix :");
    }

    private boolean login(String email, String password) {
        try {
            ResultSet rs = userDAO.getUserByEmailAndPassword(email, password);
            if (rs.next()) {
                dos.writeUTF("Connexion réussie");
                userAccount = new User(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"), rs.getString("password"));
                userDAO.setUserOnlineStatus(userAccount.getId(), true);
                mapDos.put(email, dos); // Ajouter l'utilisateur à la Map des utilisateurs connectés
                return true;
            }
            dos.writeUTF("Échec de la connexion");
            return false;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean register(String name, String email, String password, String confirmPassword) {
        try {
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                dos.writeUTF("Tous les champs doivent être remplis.");
                return false;
            }
            if (!isValidEmail(email)) {
                dos.writeUTF("Format d'email invalide.");
                return false;
            }
            if (!isValidPassword(password)) {
                dos.writeUTF("Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule et un chiffre.");
                return false;
            }
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
                    userDAO.setUserOnlineStatus(userAccount.getId(), true);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

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
                    break;
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
            dos.writeUTF("b. Envoyer un fichier\n");  // Nouvelle option
            dos.writeUTF("c. Voir la conversation\n");
            dos.writeUTF("d. Retour au menu principal\n");
            dos.writeUTF("==============================\n");
            dos.writeUTF("Veuillez entrer votre choix :");
    
            choice = dis.readLine();
            switch (choice) {
                case "a":
                    sendUserMessage();
                    break;
                case "b":
                    sendFile();  // Nouvelle méthode pour envoyer un fichier
                    break;
                case "c":
                    displayConversation();
                    break;
                case "d":
                    break;
                default:
                    dos.writeUTF("Choix invalide, veuillez réessayer");
            }
        } while (!choice.equals("d"));
    }

    private void groupMenu() throws IOException {
        String choice;
        do {
            dos.writeUTF("\n====== Menu Groupes ======\n");
            dos.writeUTF("a. Créer un groupe\n");
            dos.writeUTF("b. Rejoindre un groupe\n");
            dos.writeUTF("c. Ajouter un membre à un groupe\n");
            dos.writeUTF("d. Supprimer un membre d'un groupe\n"); // Nouvelle option
            dos.writeUTF("e. Afficher les groupes disponibles\n");
            dos.writeUTF("f. Retour au menu principal\n");
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
                    addMemberToGroup();
                    break;
                case "d":
                    removeMemberFromGroup();
                    break;
                case "e":
                    displayUserGroups();
                    break;
                case "f":
                    break;
                default:
                    dos.writeUTF("Choix invalide, veuillez réessayer");
            }
        } while (!choice.equals("f"));
    }
    private void removeMemberFromGroup() throws IOException {
        dos.writeUTF("Entrez le nom du groupe :");
        String groupName = dis.readLine();
        int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) {
            dos.writeUTF("Groupe introuvable !");
            return;
        }
    
        // Vérifier si l'utilisateur est l'admin du groupe
        if (!groupDAO.isGroupAdmin(userAccount.getId(), groupId)) {
            dos.writeUTF("Vous n'êtes pas l'administrateur de ce groupe.");
            return;
        }
    
        dos.writeUTF("Entrez l'email de l'utilisateur à supprimer :");
        String email = dis.readLine();
        int userId = userDAO.getUserIdByEmail(email);
        if (userId == -1) {
            dos.writeUTF("Utilisateur introuvable !");
            return;
        }
    
        boolean success = groupDAO.removeUserFromGroup(userId, groupId);
        dos.writeUTF(success ? "Utilisateur supprimé du groupe avec succès." : "Échec de la suppression de l'utilisateur du groupe.");
    
        // Notify the user if they are online
        String recipientEmail = userDAO.getEmailById(userId);
        if (recipientEmail != null && mapDos.containsKey(recipientEmail)) {
            DataOutputStream recipientDos = mapDos.get(recipientEmail);
            try {
                recipientDos.writeUTF("Vous avez été supprimé du groupe " + groupName + " par l'administrateur.");
                SoundPlayer.playSound("C:\\Users\\Kaoutar Iabakriman\\Desktop\\SwiftChat\\src\\utils\\notif.wav");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void displayUserGroups() throws IOException {
        List<Group> groups = groupDAO.getGroupsForUser(userAccount.getId());
        if (groups.isEmpty()) {
            dos.writeUTF("Vous n'êtes membre d'aucun groupe.");
        } else {
            dos.writeUTF("Groupes dont vous êtes membre :");
            for (Group group : groups) {
                dos.writeUTF("Nom : " + group.getName() + ", Description : " + group.getDescription());
            }
        }
    }
    
    private void addMemberToGroup() throws IOException {
        dos.writeUTF("Entrez le nom du groupe :");
        String groupName = dis.readLine();
        int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) {
            dos.writeUTF("Groupe introuvable !");
            return;
        }
    
        // Vérifier si l'utilisateur est l'admin du groupe
        if (!groupDAO.isGroupAdmin(userAccount.getId(), groupId)) {
            dos.writeUTF("Vous n'êtes pas l'administrateur de ce groupe.");
            return;
        }
    
        dos.writeUTF("Entrez l'email de l'utilisateur à ajouter :");
        String email = dis.readLine();
        int userId = userDAO.getUserIdByEmail(email);
        if (userId == -1) {
            dos.writeUTF("Utilisateur introuvable !");
            return;
        }
    
        boolean success = groupDAO.addUserToGroup(userId, groupId);
        dos.writeUTF(success ? "Utilisateur ajouté au groupe avec succès." : "Échec de l'ajout de l'utilisateur au groupe.");
    
        // Notify the user if they are online
        String recipientEmail = userDAO.getEmailById(userId);
        if (recipientEmail != null && mapDos.containsKey(recipientEmail)) {
            DataOutputStream recipientDos = mapDos.get(recipientEmail);
            try {
                recipientDos.writeUTF("Vous avez été ajouté au groupe " + groupName + " par l'administrateur.");
                SoundPlayer.playSound("C:\\Users\\Kaoutar Iabakriman\\Desktop\\SwiftChat\\src\\utils\\notif.wav");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void joinGroup() throws IOException {
        dos.writeUTF("Entrez le nom du groupe :");
        String groupName = dis.readLine();
        int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) {
            dos.writeUTF("Groupe introuvable !");
            return;
        }
    
        boolean success = groupDAO.addUserToGroup(userAccount.getId(), groupId);
        dos.writeUTF(success ? "Vous avez rejoint le groupe avec succès." : "Échec de la jonction au groupe.");
    }
   
    private void sendFile() throws IOException {
        // 1. Demander le surnom du destinataire
        this.dos.writeUTF("Entrez le surnom du destinataire :");
        String nickname = this.dis.readLine();
    
        // 2. Vérifier si le destinataire existe
        int recipientUserId = contactDAO.getUserIdByNickname(userAccount.getId(), nickname);
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
    
        // 4. Demander le chemin du fichier à envoyer
        this.dos.writeUTF("Entrez le chemin complet du fichier à envoyer :");
        String filePath = this.dis.readLine();
    
        File file = new File(filePath);
        if (!file.exists()) {
            this.dos.writeUTF("Fichier introuvable !");
            return;
        }
    
        // 5. Insérer le message de fichier dans la base de données
        int messageId = messageDAO.insertFileMessage(userAccount.getId(), recipientUserId, file.getName());
        if (messageId == -1) {
            this.dos.writeUTF("Erreur : Impossible d'enregistrer le message de fichier.");
            return;
        }
    
        // 6. Envoyer le fichier
        try (FileInputStream fis = new FileInputStream(file)) {
            // Envoyer le nom du fichier et sa taille
            this.dos.writeUTF(file.getName());
            this.dos.writeLong(file.length());
    
            // Envoyer le fichier par morceaux
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                this.dos.write(buffer, 0, bytesRead);
            }
    
            this.dos.writeUTF("Fichier envoyé avec succès.");
        } catch (IOException e) {
            e.printStackTrace();
            this.dos.writeUTF("Échec de l'envoi du fichier.");
        }
    
        // 7. Notifier le destinataire s'il est en ligne
        String recipientEmail = userDAO.getEmailById(recipientUserId);
        if (recipientEmail != null && mapDos.containsKey(recipientEmail)) {
            DataOutputStream recipientDos = mapDos.get(recipientEmail);
            try {
                recipientDos.writeUTF("Nouveau fichier de " + userAccount.getEmail() + " : " + file.getName());
                SoundPlayer.playSound("C:\\Users\\Kaoutar Iabakriman\\Desktop\\SwiftChat\\src\\utils\\notif.wav");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Stocker le message en attente si le destinataire est hors ligne
            boolean pendingStored = messageDAO.storePendingMessage(recipientUserId, messageId);
            if (pendingStored) {
                this.dos.writeUTF("Le destinataire est déconnecté. Le fichier sera délivré à sa reconnexion.");
            } else {
                this.dos.writeUTF("Échec de l'enregistrement du fichier en attente.");
            }
        }
    }
    private void displayConversation() throws IOException {
        dos.writeUTF("Entrez le surnom du contact pour voir la conversation :");
        String nickname = dis.readLine();

        // Récupérer l'ID du contact à partir du surnom
        int contactUserId = contactDAO.getUserIdByNickname(userAccount.getId(), nickname);
        if (contactUserId == -1) {
            dos.writeUTF("Contact introuvable !");
            return;
        }

        // Récupérer la conversation depuis la table `messages`
        List<Message> messages = messageDAO.getConversation(userAccount.getId(), contactUserId);
        if (messages.isEmpty()) {
            dos.writeUTF("Aucun message trouvé.");
        } else {
            // Afficher les messages de la conversation
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

    private void sendUserMessage() throws IOException {
        // 1. Demander le surnom du destinataire
        this.dos.writeUTF("Entrez le surnom du destinataire :");
        String nickname = this.dis.readLine();
    
        // 2. Vérifier si le destinataire existe
        int recipientUserId = contactDAO.getUserIdByNickname(userAccount.getId(), nickname);
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
    
        // 5. Insérer le message dans la table `messages`
        int messageId = messageDAO.insertMessage(userAccount.getId(), recipientUserId, message);
        if (messageId == -1) {
            this.dos.writeUTF("Erreur : Impossible d'enregistrer le message.");
            return;
        }
    
        // 6. Récupérer l'email du destinataire pour vérifier s'il est en ligne
        String recipientEmail = userDAO.getEmailById(recipientUserId);
        if (recipientEmail == null) {
            this.dos.writeUTF("Erreur : Impossible de récupérer l'email du destinataire.");
            return;
        }
    
        // 7. Vérifier si le destinataire est en ligne
        if (mapDos.containsKey(recipientEmail)) {
            // Envoyer le message en temps réel
            DataOutputStream recipientDos = mapDos.get(recipientEmail);
            try {
                recipientDos.writeUTF("Nouveau message de " + userAccount.getEmail() + " : " + message);
                this.dos.writeUTF("Message envoyé avec succès.");
    
                // Jouer un son pour le destinataire
                SoundPlayer.playSound("C:\\Users\\Kaoutar Iabakriman\\Desktop\\SwiftChat\\src\\utils\\notif.wav");
            } catch (IOException e) {
                e.printStackTrace();
                this.dos.writeUTF("Échec de l'envoi du message.");
            }
        } else {
            // 8. Stocker le message en attente dans `pending_messages`
            boolean pendingStored = messageDAO.storePendingMessage(recipientUserId, messageId);
            if (pendingStored) {
                this.dos.writeUTF("Le destinataire est déconnecté. Le message sera délivré à sa reconnexion.");
            } else {
                this.dos.writeUTF("Échec de l'enregistrement du message en attente.");
            }
        }
    }

    private void receiveAndDeleteMessages() {
        try {
            // Récupérer les messages en attente pour l'utilisateur connecté
            var messages = messageDAO.getPendingMessagesForUser(userAccount.getId());
            if (messages.isEmpty()) {
                dos.writeUTF("Aucun nouveau message.");
            } else {
                // Afficher les messages en attente
                for (var msg : messages) {
                    if ("file".equals(msg.getMessageType())) {
                        dos.writeUTF("Nouveau fichier de " + msg.getSenderEmail() + " : " + msg.getFileName());
                    } else {
                        dos.writeUTF("Nouveau message de " + msg.getSenderEmail() + " à " + msg.getDate() + " : " + msg.getMessage());
                    }
                }
                // Supprimer les messages en attente après les avoir affichés
                messageDAO.deletePendingMessagesForUser(userAccount.getId());
            }
        } catch (IOException e) {
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