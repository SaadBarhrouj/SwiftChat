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
                boolean hasNewMessages = receiveAndDeleteMessages();
                if (!hasNewMessages) {
                    dos.writeUTF("Aucun nouveau message.");
                }

                while (true) {
                    userMenu();
                    String messageType = dis.readUTF();
                    switch (messageType) {
                        case "text":
                            sendUserMessage();
                            break;
                        case "file":
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
                    this.dos.writeUTF("Veuillez entrer votre email :");
                    email = this.dis.readLine();
                    this.dos.writeUTF("Veuillez entrer votre mot de passe :");
                    password = this.dis.readLine();
                    Auth = login(email, password);
                    if (Auth) {
                         boolean hasNewMessagesLogin = receiveAndDeleteMessages();
                        if (!hasNewMessagesLogin) {
                           
                        }
                    }
                    break;
                default:
                    this.dos.writeUTF("Choix invalide, veuillez reessayer");
            }
        } while (!Auth);
    }

//a verifie !!!!!
    private void receiveFile() throws IOException {
        new Thread(() -> {
            try {
                String fileName = dis.readUTF();
                long fileSize = dis.readLong();

                File file = new File("received_" + fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalBytesRead = 0;
                    while (totalBytesRead < fileSize) {
                        bytesRead = dis.read(buffer, 0, buffer.length);
                        totalBytesRead += bytesRead;
                        fos.write(buffer, 0, bytesRead);
                    }

                    dos.writeUTF("Fichier recu avec succes : " + file.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                    dos.writeUTF("Echec de la reception du fichier.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    


    private boolean login(String email, String password) {
        try {
            ResultSet rs = userDAO.getUserByEmailAndPassword(email, password);
            if (rs.next()) {
                dos.writeUTF("Connexion reussie");
                userAccount = new User(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"), rs.getString("password"));
                userDAO.setUserOnlineStatus(userAccount.getId(), true);
                mapDos.put(email, dos);
                return true;
            }
            dos.writeUTF("Echec de la connexion");
            return false;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean register(String name, String email, String password, String confirmPassword) {
        try {
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                dos.writeUTF("Tous les champs doivent etre remplis.");
                return false;
            }
            if (!isValidEmail(email)) {
                dos.writeUTF("Format d'email invalide.");
                return false;
            }
            if (!isValidPassword(password)) {
                dos.writeUTF("Le mot de passe doit contenir au moins 8 caracteres, une majuscule, une minuscule et un chiffre.");
                return false;
            }
            if (!password.equals(confirmPassword)) {
                dos.writeUTF("Les mots de passe ne correspondent pas.");
                return false;
            }
            if (userDAO.userExists(email)) {
                dos.writeUTF("Email deja utilise");
                return false;
            }
            if (userDAO.insertUser(name, email, password, confirmPassword)) {
                ResultSet rs = userDAO.getUserByEmailAndPassword(email, password);
                if (rs.next()) {
                    userAccount = new User(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"), rs.getString("password"));
                    dos.writeUTF("Inscription reussie");
                    userDAO.setUserOnlineStatus(userAccount.getId(), true);
                    mapDos.put(email, dos);
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


    private void showMainMenu() throws IOException {
        StringBuilder menu = new StringBuilder();
        menu.append("====== Menu Principal ======\r\n");
        menu.append("a. S'inscrire\r\n");
        menu.append("b. Se connecter\r\n");
        menu.append("============================\r\n");
        menu.append("Veuillez entrer votre choix :");
        this.dos.writeUTF(menu.toString());
    }
    
    private void userMenu() throws IOException {
        String choice;
        do {
            StringBuilder menu = new StringBuilder();
            menu.append("\r\n====== Menu Utilisateur ======\r\n");
            menu.append("1. Gestion des Contacts\r\n");
            menu.append("2. Gestion des Messages\r\n");
            menu.append("3. Gestion des Groupes\r\n");
            menu.append("4. Mettre a jour le profil\r\n");
            menu.append("5. Se deconnecter\r\n");
            menu.append("==============================\r\n");
            menu.append("Veuillez entrer votre choix :");
            dos.writeUTF(menu.toString());
    
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
                    dos.writeUTF("Choix invalide, veuillez reessayer\r\n");
            }
        } while (!choice.equals("5"));
    }
    
    private void contactMenu() throws IOException {
        String choice;
        do {
            StringBuilder menu = new StringBuilder();
            menu.append("\r\n====== Menu Contacts ======\r\n");
            menu.append("a. Ajouter un contact\r\n");
            menu.append("b. Supprimer un contact\r\n");
            menu.append("c. Modifier le surnom\r\n");
            menu.append("d. Lister les contacts\r\n");
            menu.append("e. Retour au menu principal\r\n");
            menu.append("==============================\r\n");
            menu.append("Veuillez entrer votre choix :");
            dos.writeUTF(menu.toString());
    
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
                    dos.writeUTF("Choix invalide, veuillez reessayer\r\n");
            }
        } while (!choice.equals("e"));
    }
    
    private void messageMenu() throws IOException {
        String choice;
        do {
            StringBuilder menu = new StringBuilder();
            menu.append("\r\n====== Menu Messages ======\r\n");
            menu.append("a. Envoyer un message\r\n");
            menu.append("b. Envoyer un fichier\r\n");
            menu.append("c. Voir la conversation\r\n");
            menu.append("d. Retour au menu principal\r\n");
            menu.append("==============================\r\n");
            menu.append("Veuillez entrer votre choix :");
            dos.writeUTF(menu.toString());
    
            choice = dis.readLine();
            switch (choice) {
                case "a":
                    sendUserMessage();
                    break;
                case "b":
                    sendFile();
                    break;
                case "c":
                    displayConversation();
                    break;
                case "d":
                    break;
                default:
                    dos.writeUTF("Choix invalide, veuillez veuillez reessayer\r\n");
            }
        } while (!choice.equals("d"));
    }
    
    private void groupMenu() throws IOException {
        String choice;
        do {
            StringBuilder menu = new StringBuilder();
            menu.append("\r\n====== Menu Groupes ======\r\n");
            menu.append("a. Creer un groupe\r\n");
            menu.append("b. Rejoindre un groupe\r\n");
            menu.append("c. Ajouter un membre a un groupe\r\n");
            menu.append("d. Supprimer un membre d'un groupe\r\n");
            menu.append("e. Afficher les groupes disponibles\r\n");
            menu.append("f. Envoyer un message a un groupe\r\n");
            menu.append("g. Retour au menu principal\r\n");
            menu.append("==============================\r\n");
            menu.append("Veuillez entrer votre choix :");
            dos.writeUTF(menu.toString());
    
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
                    sendGroupMessage();
                    break;
                case "g":
                    break;
                default:
                    dos.writeUTF("Choix invalide, veuillez reessayer\r\n");
            }
        } while (!choice.equals("g"));
    }
    private void removeMemberFromGroup() throws IOException {
        dos.writeUTF("Entrez le nom du groupe :");
        String groupName = dis.readLine();
        int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) {
            dos.writeUTF("Groupe introuvable");
            return;
        }

        if (!groupDAO.isGroupAdmin(userAccount.getId(), groupId)) {
            dos.writeUTF("Pas administrateur de ce groupe.");
            return;
        }

        dos.writeUTF("Entrez email de utilisateur a supprimer :");
        String email = dis.readLine();
        int userId = userDAO.getUserIdByEmail(email);
        if (userId == -1) {
            dos.writeUTF("Utilisateur introuvable ");
            return;
        }

        boolean success = groupDAO.removeUserFromGroup(userId, groupId);
        dos.writeUTF(success ? "Utilisateur supprime du groupe avec succes." : "Echec de la suppression de utilisateur du groupe.");

        String recipientEmail = userDAO.getEmailById(userId);
        if (recipientEmail != null && mapDos.containsKey(recipientEmail)) {
            DataOutputStream recipientDos = mapDos.get(recipientEmail);
            try {
                recipientDos.writeUTF("Vous avez ete supprime du groupe " + groupName + " par l'administrateur.");
                SoundPlayer.playSound("C:\\\\Users\\\\Lenovo\\\\IdeaProjects\\\\ChatApplication\\\\src\\\\utils\\\\notif.wav");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void displayUserGroups() throws IOException {
        List<Group> groups = groupDAO.getGroupsForUser(userAccount.getId());
        if (groups.isEmpty()) {
            dos.writeUTF("Vous n'etes membre d'aucun groupe.");
        } else {
            dos.writeUTF("Groupes dont vous etes membre :");
            StringBuilder groupList = new StringBuilder(); // Use StringBuilder for efficient string building

            for (Group group : groups) {
                groupList.append("Nom: ").append(group.getName()).append(", Description: ").append(group.getDescription()).append("\r\n"); // Append each group info
            }

            dos.writeUTF(groupList.toString()); // Send the complete list as one message.
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


        if (!groupDAO.isGroupAdmin(userAccount.getId(), groupId)) {
            dos.writeUTF("Vous n'etes pas l'administrateur de ce groupe.");
            return;
        }

        dos.writeUTF("Entrez l'email de l'utilisateur a ajouter :");
        String email = dis.readLine();
        int userId = userDAO.getUserIdByEmail(email);
        if (userId == -1) {
            dos.writeUTF("Utilisateur introuvable !");
            return;
        }

        boolean success = groupDAO.addUserToGroup(userId, groupId);
        dos.writeUTF(success ? "Utilisateur ajoute au groupe avec succes." : "Echec de l'ajout de l'utilisateur au groupe.");

        String recipientEmail = userDAO.getEmailById(userId);
        if (recipientEmail != null && mapDos.containsKey(recipientEmail)) {
            DataOutputStream recipientDos = mapDos.get(recipientEmail);
            try {
                recipientDos.writeUTF("Vous avez ete ajoute au groupe " + groupName + " par l'administrateur.");
                SoundPlayer.playSound("C:\\\\Users\\\\Lenovo\\\\IdeaProjects\\\\ChatApplication\\\\src\\\\utils\\\\notif.wav");
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
        dos.writeUTF(success ? "Vous avez rejoint le groupe avec succes." : "Echec de la jonction au groupe.");
    }



    private void sendFile() throws IOException {
        this.dos.writeUTF("Entrez le surnom du destinataire :");
        String nickname = this.dis.readLine();

        int recipientUserId = contactDAO.getUserIdByNickname(userAccount.getId(), nickname);
        if (recipientUserId == -1) {
            this.dos.writeUTF("Utilisateur introuvable !");
            return;
        }

        if (!contactDAO.areContacts(userAccount.getId(), recipientUserId)) {
            this.dos.writeUTF("Le destinataire n'est pas dans votre liste de contacts.");
            this.dos.writeUTF("Souhaitez-vous ajouter ce contact ? (oui/non)");
            String response = this.dis.readLine();
            if (response.equalsIgnoreCase("oui")) {
                handleAddContact();
                return;
            } else {
                this.dos.writeUTF("Retour au menu principal.");
                return;
            }
        }

        this.dos.writeUTF("Entrez le chemin complet du fichier a envoyer :");
        String filePath = this.dis.readLine();

        File file = new File(filePath);
        if (!file.exists()) {
            this.dos.writeUTF("Fichier introuvable !");
            return;
        }

        int messageId = messageDAO.insertFileMessage(userAccount.getId(), recipientUserId, file.getName());
        if (messageId == -1) {
            this.dos.writeUTF("Erreur : Impossible d'enregistrer le message de fichier.");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            this.dos.writeUTF(file.getName());
            this.dos.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                this.dos.write(buffer, 0, bytesRead);
            }

            this.dos.writeUTF("Fichier envoye avec succes.");
        } catch (IOException e) {
            e.printStackTrace();
            this.dos.writeUTF("Echec de l'envoi du fichier.");
        }

        String recipientEmail = userDAO.getEmailById(recipientUserId);
        if (recipientEmail != null && mapDos.containsKey(recipientEmail)) {
            DataOutputStream recipientDos = mapDos.get(recipientEmail);
            try {
                recipientDos.writeUTF("Nouveau fichier de " + userAccount.getEmail() + " : " + file.getName());
                SoundPlayer.playSound("C:\\\\Users\\\\Lenovo\\\\IdeaProjects\\\\ChatApplication\\\\src\\\\utils\\\\notif.wav");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            boolean pendingStored = messageDAO.storePendingMessage(recipientUserId, messageId);
            if (pendingStored) {
                this.dos.writeUTF("Le destinataire est deconnecte. Le fichier sera delivre a sa reconnexion.");
            } else {
                this.dos.writeUTF("Echec de l'enregistrement du fichier en attente.");
            }
        }
    }


    private void displayConversation() throws IOException {
          dos.writeUTF("Entrez le surnom du contact pour voir la conversation :");
        String nickname = dis.readLine();

        int contactUserId = contactDAO.getUserIdByNickname(userAccount.getId(), nickname);
        if (contactUserId == -1) {
            dos.writeUTF("Contact introuvable !");
            return;
        }

        List<Message> messages = messageDAO.getConversation(userAccount.getId(), contactUserId);
        String contactEmail = userDAO.getEmailById(contactUserId);
        String status = mapDos.containsKey(contactEmail) ? "en ligne" : "hors ligne";

        if (messages.isEmpty()) {
            dos.writeUTF("Aucun message trouve avec " + nickname + " (" + status + ").");
        } else {
            StringBuilder conversation = new StringBuilder(); // Use StringBuilder for efficient string building

            for (Message msg : messages) {
                conversation.append("De ").append(msg.getSenderEmail()).append(" a ").append(msg.getDate()).append(" : ").append(msg.getMessage()).append("\r\n");  //Append \r\n for new line
            }
             dos.writeUTF(conversation.toString()); //Send one combined String
        }
    }


    private void sendGroupMessage() throws IOException {
        dos.writeUTF("Entrez le nom du groupe :");
        String groupName = dis.readLine();
        int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) {
            dos.writeUTF("Groupe introuvable !");
            return;
        }

        dos.writeUTF("Entrez votre message :");
        String message = dis.readLine();

        int messageId = messageDAO.insertGroupMessage(userAccount.getId(), groupId, message);
        if (messageId == -1) {
            dos.writeUTF("Erreur : Impossible d'enregistrer le message.");
            return;
        }

        List<Integer> memberIds = groupDAO.getGroupMembers(groupId);
        for (int memberId : memberIds) {
            String recipientEmail = userDAO.getEmailById(memberId);
            if (recipientEmail != null && mapDos.containsKey(recipientEmail)) {
                DataOutputStream recipientDos = mapDos.get(recipientEmail);
                try {
                    recipientDos.writeUTF("Nouveau message dans le groupe " + groupName + " de " + userAccount.getEmail() + " : " + message);
                    SoundPlayer.playSound("C:\\\\Users\\\\Lenovo\\\\IdeaProjects\\\\ChatApplication\\\\src\\\\utils\\\\notif.wav");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                boolean pendingStored = messageDAO.storePendingMessage(memberId, messageId);
                if (pendingStored) {
                    dos.writeUTF("Le message sera delivre a la reconnexion des membres hors ligne.");
                } else {
                    dos.writeUTF("Echec de l'enregistrement du message en attente.");
                }
            }
        }
        dos.writeUTF("Message envoye au groupe avec succes.");
    }
    private void handleAddContact() throws IOException {
        this.dos.writeUTF("Entrez l'email du contact :");
        String email = this.dis.readLine();

        int contactUserId = userDAO.getUserIdByEmail(email);
        if (contactUserId == -1) {
            this.dos.writeUTF("Utilisateur introuvable !");
            return;
        }

        this.dos.writeUTF("Entrez le surnom du contact :");
        String nickname = this.dis.readLine();

        boolean success = contactDAO.addContact(userAccount.getId(), contactUserId, nickname);
        this.dos.writeUTF(success ? "Contact ajoute avec succes !" : "Echec de l'ajout du contact.");
    }



    private void handleDeleteContact() throws IOException {
        dos.writeUTF("Entrez le surnom du contact a supprimer :");
        String nickname = dis.readLine();

        int contactUserId = contactDAO.getUserIdByNickname(userAccount.getId(), nickname);
        if (contactUserId == -1) {
            dos.writeUTF("Aucun contact trouve avec ce surnom.");
            return;
        }

        boolean success = contactDAO.deleteContact(userAccount.getId(), contactUserId);
        dos.writeUTF(success ? "Contact supprime avec succes." : "Erreur lors de la suppression.");
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
        dos.writeUTF(success ? "Surnom mis a jour" : "Echec de la mise a jour");
    }

    private void handleListContacts() throws IOException {
        List<Contact> contacts = contactDAO.getContacts(userAccount.getId());
        if (contacts.isEmpty()) {
            dos.writeUTF("Aucun contact trouve");
            return;
        }
        StringBuilder sb = new StringBuilder("Liste des contacts :\r\n");
        for (Contact contact : contacts) {
            String email = userDAO.getEmailById(contact.getContactUserId());
            String status = mapDos.containsKey(email) ? "en ligne" : "hors ligne";
            sb.append(" ").append(contact.getNickname() != null ? contact.getNickname() : email)
                    .append(" (").append(status).append(")")
                    .append("\n");
                    sb.append("\r\n");
        }
        dos.writeUTF(sb.toString());
    }


    private void sendUserMessage() throws IOException {
        this.dos.writeUTF("Entrez le surnom du destinataire :");
        String nickname = this.dis.readLine();

        int recipientUserId = contactDAO.getUserIdByNickname(userAccount.getId(), nickname);
        if (recipientUserId == -1) {
            this.dos.writeUTF("Le destinataire n'est pas dans votre liste de contacts.");
            this.dos.writeUTF("Souhaitez-vous ajouter ce contact ? (oui/non)");
            String response = this.dis.readLine();

            if (response.equalsIgnoreCase("oui")) {
                handleAddContact();
                recipientUserId = contactDAO.getUserIdByNickname(userAccount.getId(), nickname);
                if (recipientUserId == -1) {
                    this.dos.writeUTF("Erreur : Le contact n'a pas pu etre ajoute.");
                    return;
                }
            } else {
                this.dos.writeUTF("Retour au menu principal.");
                return;
            }
        }

        this.dos.writeUTF("Entrez votre message :");
        String message = this.dis.readLine();

        int messageId = messageDAO.insertMessage(userAccount.getId(), recipientUserId, message);
        if (messageId == -1) {
            this.dos.writeUTF("Erreur : Impossible d'enregistrer le message.");
            return;
        }

        String recipientEmail = userDAO.getEmailById(recipientUserId);
        if (recipientEmail == null) {
            this.dos.writeUTF("Erreur : Impossible de recuperer l'email du destinataire.");
            return;
        }

        if (mapDos.containsKey(recipientEmail)) {
            DataOutputStream recipientDos = mapDos.get(recipientEmail);
            try {
                recipientDos.writeUTF("Nouveau message de " + userAccount.getEmail() + " : " + message);
                this.dos.writeUTF("Message envoye avec succes.");

                SoundPlayer.playSound("C:\\\\Users\\\\Lenovo\\\\IdeaProjects\\\\ChatApplication\\\\src\\\\utils\\\\notif.wav");
            } catch (IOException e) {
                e.printStackTrace();
                this.dos.writeUTF("Echec de l'envoi du message.");
            }
        } else {
            boolean pendingStored = messageDAO.storePendingMessage(recipientUserId, messageId);
            if (pendingStored) {
                this.dos.writeUTF("Le destinataire est deconnecte. Le message sera delivre a sa reconnexion.");
            } else {
                this.dos.writeUTF("Echec de l'enregistrement du message en attente.");
            }
        }
    }
    private boolean receiveAndDeleteMessages() {
        boolean hasNewMessages = false;
        try {
            var messages = messageDAO.getPendingMessagesForUser(userAccount.getId());
            System.out.println(userAccount.getId());
            System.out.println("Messages en attente recuperes : " + messages.size());
            if (!messages.isEmpty()) {
                hasNewMessages = true;
                StringBuilder messageList = new StringBuilder();
                for (var msg : messages) {
                   String messageInfo;
                    if ("file".equals(msg.getMessageType())) {
                       messageInfo = "Nouveau fichier de " + msg.getSenderEmail() + " : " + msg.getFileName();
                    } else {
                        messageInfo = "Nouveau message de " + msg.getSenderEmail() + " a " + msg.getDate() + " : " + msg.getMessage();
                    }
                    messageList.append(messageInfo).append("\r\n"); // Append each group info

                    SoundPlayer.playSound("C:\\\\Users\\\\Lenovo\\\\IdeaProjects\\\\ChatApplication\\\\src\\\\utils\\\\notif.wav");
                }
                dos.writeUTF(messageList.toString());
                messageDAO.deletePendingMessagesForUser(userAccount.getId());

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hasNewMessages;
    }
    private void createGroup() throws IOException {
        dos.writeUTF("Entrez le nom du groupe :");
        String name = dis.readLine();
        dos.writeUTF("Entrez la description du groupe :");
        String description = dis.readLine();
        if (groupDAO.createGroup(name, description, userAccount.getId())) {
            dos.writeUTF("Groupe cree avec succes.");
        } else {
            dos.writeUTF("Echec de la creation du groupe.");
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
        dos.writeUTF("Profil mis a jour avec succes !");
    }


    private void logout() {
        try {
            dos.writeUTF("Deconnexion en cours...");
            Auth = false;
            if (userAccount != null) {
                mapDos.remove(userAccount.getEmail());
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