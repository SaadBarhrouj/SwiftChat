package Server;

import Dao.ContactDAO;
import Entities.Contact;
import Entities.User;
import Dao.UserDAO;
import Dao.DatabaseConnection;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * La classe ClientHandler gère la communication avec un client connecté au serveur.
 * Elle permet la gestion de l'authentification (inscription et connexion), ainsi que la gestion des contacts.
 */
public class ClientHandler extends Thread {
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final Socket commthread;
    private boolean Auth;
    private User userAccount;
    private UserDAO userDAO;
    private Connection conn;
    private Statement stmt;
    private ResultSet rs;
    private ContactDAO contactDAO;

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
                String choice = "";
                do {
                    this.showMenu();
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

                if (Auth) {
                    // Une fois authentifié, afficher le menu utilisateur
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

    /**
     * Affiche le menu principal (inscription et connexion).
     *
     * @throws IOException Si une erreur de communication se produit.
     */
    private void showMenu() throws IOException {
        this.dos.writeUTF("\n====== Menu Principal ======\n");
        this.dos.writeUTF("a. S'inscrire\n");
        this.dos.writeUTF("b. Se connecter\n");
        this.dos.writeUTF("============================\n");
        this.dos.writeUTF("Veuillez entrer votre choix :");
    }

    /**
     * Méthode de connexion. Valide l'email et le mot de passe de l'utilisateur.
     *
     * @param email L'email de l'utilisateur.
     * @param password Le mot de passe de l'utilisateur.
     * @return true si la connexion réussit, false sinon.
     */
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

    /**
     * Méthode d'inscription. Vérifie si l'utilisateur existe déjà et enregistre un nouvel utilisateur.
     *
     * @param name Le nom de l'utilisateur.
     * @param email L'email de l'utilisateur.
     * @param password Le mot de passe de l'utilisateur.
     * @param confirmPassword La confirmation du mot de passe.
     * @return true si l'inscription réussit, false sinon.
     */
    private boolean register(String name, String email, String password, String confirmPassword) {
        try {
            if (userDAO.userExists(email)) {
                dos.writeUTF("Email déjà utilisé");
                return false;
            } else {
                if (userDAO.insertUser(name, email, password, confirmPassword)) {
                    // Récupérer le nouvel utilisateur créé
                    ResultSet rs = userDAO.getUserByEmailAndPassword(email, password);
                    if (rs.next()) {
                        userAccount = new User(
                                rs.getInt("user_id"),
                                rs.getString("name"),
                                rs.getString("email"),
                                rs.getString("password")
                        );
                        dos.writeUTF("Inscription réussie");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Affiche le menu utilisateur une fois l'utilisateur connecté.
     * Gère les actions possibles : ajout, suppression, mise à jour et liste des contacts.
     *
     * @throws IOException Si une erreur de communication se produit.
     */
    private void userMenu() throws IOException {
        String choice;
        do {
            dos.writeUTF("\n====== Menu Utilisateur ======\n");
            dos.writeUTF("c. Ajouter un contact\n");
            dos.writeUTF("d. Supprimer un contact\n");
            dos.writeUTF("e. Modifier le surnom\n");
            dos.writeUTF("f. Lister les contacts\n");
            dos.writeUTF("g. Se déconnecter\n");
            dos.writeUTF("==============================\n");
            dos.writeUTF("Veuillez entrer votre choix :");

            choice = dis.readLine();
            switch (choice) {
                case "c":
                    handleAddContact();
                    break;
                case "d":
                    handleDeleteContact();
                    break;
                case "e":
                    handleUpdateNickname();
                    break;
                case "f":
                    handleListContacts();
                    break;
                case "g":
                    logout();
                    break;
                default:
                    dos.writeUTF("Choix invalide, veuillez réessayer");
            }
        } while (!choice.equals("g"));
    }

    /**
     * Gère l'ajout d'un contact.
     * Demande à l'utilisateur l'email du contact et un surnom optionnel, puis ajoute le contact à la base de données.
     *
     * @throws IOException Si une erreur de communication se produit.
     */
    private void handleAddContact() throws IOException {
        dos.writeUTF("Entrez l'email du contact :");
        String email = dis.readLine();

        // Vérifier si l'utilisateur existe
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

    /**
     * Gère la suppression d'un contact.
     * Demande à l'utilisateur l'email du contact à supprimer et le supprime de la base de données.
     *
     * @throws IOException Si une erreur de communication se produit.
     */
    private void handleDeleteContact() throws IOException {
        dos.writeUTF("Entrez l'email du contact à supprimer :");
        String email = dis.readLine();

        int contactUserId = userDAO.getUserIdByEmail(email);
        if (contactUserId == -1) {
            dos.writeUTF("Aucun utilisateur trouvé avec cet email");
            return;
        }

        boolean success = contactDAO.deleteContact(userAccount.getId(), contactUserId);
        dos.writeUTF(success ? "Contact supprime avec succes" : " Erreur lors de la suppression");
    }

    /**
     * Gère la mise à jour du surnom d'un contact.
     * Demande à l'utilisateur l'email du contact et le nouveau surnom.
     *
     * @throws IOException Si une erreur de communication se produit.
     */
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
        dos.writeUTF(success ? "Surnom mis à jour" : "Echec de la mise à jour");
    }

    /**
     * Affiche la liste des contacts de l'utilisateur.
     *
     * @throws IOException Si une erreur de communication se produit.
     */
    private void handleListContacts() throws IOException {
        List<Contact> contacts = contactDAO.getContacts(userAccount.getId());
        if (contacts.isEmpty()) {
            dos.writeUTF("📭 Aucun contact trouvé");
            return;
        }

        StringBuilder sb = new StringBuilder("Liste des contacts :\n");
        UserDAO userDAO = new UserDAO();

        for (Contact contact : contacts) {
            String email = userDAO.getEmailById(contact.getContactUserId());
            sb.append("➤ ").append(email)
                    .append(contact.getNickname() != null ? " (" + contact.getNickname() + ")" : "")
                    .append("\n");
        }

        dos.writeUTF(sb.toString());
    }

    /**
     * Gère la déconnexion de l'utilisateur.
     *
     * @throws IOException Si une erreur de communication se produit.
     */
    private void logout() throws IOException {
        dos.writeUTF("Déconnexion en cours...");
        Auth = false;
        if (userAccount != null) {
            userAccount.disconnect();
        }
        dis.close();
        dos.close();
        commthread.close();
    }

    /**
     * Gère les erreurs lors de la fermeture de la connexion.
     */
    private void error() {
        try {
            if (this.Auth && this.userAccount != null) {
                this.userAccount.disconnect();
            }
            this.dis.close();
            this.dos.close();
            this.commthread.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
