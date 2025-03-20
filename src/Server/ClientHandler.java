package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * ClientHandler est une classe qui gère la communication avec un client connecté.
 * Elle est responsable de l'authentification et de l'enregistrement des utilisateurs.
 * Chaque instance de cette classe est exécutée dans un thread séparé pour permettre
 * une communication simultanée avec plusieurs clients.
 */
public class ClientHandler extends Thread {
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final Socket clientSocket;
    private boolean isAuthenticated;
    private User userAccount;
    private UserDAO userDAO;
    private Connection conn;
    private static Map<String, DataOutputStream> connectedUsers = new HashMap<>();

    /**
     * Constructeur de la classe ClientHandler.
     *
     * @param socket Le socket du client connecté.
     * @param dis    Le flux d'entrée pour lire les données du client.
     * @param dos    Le flux de sortie pour envoyer des données au client.
     */
    public ClientHandler(Socket socket, DataInputStream dis, DataOutputStream dos) {
        this.clientSocket = socket;
        this.dis = dis;
        this.dos = dos;
        this.isAuthenticated = false;
        this.userDAO = new UserDAO();
        this.conn = DatabaseConnection.getConnection();
        this.start(); // Démarre le thread dès que l'objet est créé
    }

    @Override
    public void run() {
        try {
            authenticateUser();
            if (isAuthenticated) {
                // L'utilisateur est authentifié, on peut maintenant gérer sa session
                handleUserSession();
            }
        } catch (IOException e) {
            System.err.println("Error handling client connection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    /**
     * Authentifie l'utilisateur en lui demandant de se connecter ou de s'inscrire.
     *
     * @throws IOException En cas d'erreur de communication avec le client.
     */
    private void authenticateUser() throws IOException {
        String choice;
        do {
            showMainMenu();
            choice = dis.readLine();

            String name, email, password, confirmPassword;
            switch (choice) {
                case "a":
                    // Inscription
                    dos.writeUTF("Veuillez entrer votre nom :");
                    name = dis.readLine();
                    dos.writeUTF("Veuillez entrer votre email :");
                    email = dis.readLine();
                    dos.writeUTF("Veuillez entrer votre mot de passe :");
                    password = dis.readLine();
                    dos.writeUTF("Veuillez confirmer votre mot de passe :");
                    confirmPassword = dis.readLine();
                    isAuthenticated = register(name, email, password, confirmPassword);
                    break;
                case "b":
                    // Connexion
                    dos.writeUTF("Veuillez entrer votre email :");
                    email = dis.readLine();
                    dos.writeUTF("Veuillez entrer votre mot de passe :");
                    password = dis.readLine();
                    isAuthenticated = login(email, password);
                    break;
                default:
                    dos.writeUTF("Choix invalide, veuillez réessayer.");
            }
        } while (!isAuthenticated);
    }

    /**
     * Affiche le menu principal pour l'authentification.
     *
     * @throws IOException En cas d'erreur de communication avec le client.
     */
    private void showMainMenu() throws IOException {
        dos.writeUTF("\n====== Menu Principal ======\n");
        dos.writeUTF("a. S'inscrire\n");
        dos.writeUTF("b. Se connecter\n");
        dos.writeUTF("============================\n");
        dos.writeUTF("Veuillez entrer votre choix :");
    }

    /**
     * Gère la session de l'utilisateur après une authentification réussie.
     *
     * @throws IOException En cas d'erreur de communication avec le client.
     */
    private void handleUserSession() throws IOException {
        dos.writeUTF("Authentification réussie. Bienvenue, " + userAccount.getName() + "!");
        // Ici, vous pouvez ajouter des fonctionnalités supplémentaires pour la session utilisateur.
    }

    /**
     * Enregistre un nouvel utilisateur.
     *
     * @param name           Le nom de l'utilisateur.
     * @param email          L'email de l'utilisateur.
     * @param password       Le mot de passe de l'utilisateur.
     * @param confirmPassword La confirmation du mot de passe.
     * @return true si l'enregistrement est réussi, false sinon.
     */
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
                dos.writeUTF("Email déjà utilisé.");
                return false;
            }
            if (userDAO.insertUser(name, email, password)) {
                ResultSet rs = userDAO.getUserByEmailAndPassword(email, password);
                if (rs.next()) {
                    userAccount = new User(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"), rs.getString("password"));
                    dos.writeUTF("Inscription réussie.");
                    connectedUsers.put(email, dos); // Ajouter l'utilisateur à la Map des utilisateurs connectés
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Connecte un utilisateur existant.
     *
     * @param email    L'email de l'utilisateur.
     * @param password Le mot de passe de l'utilisateur.
     * @return true si la connexion est réussie, false sinon.
     */
    private boolean login(String email, String password) {
        try {
            ResultSet rs = userDAO.getUserByEmailAndPassword(email, password);
            if (rs.next()) {
                dos.writeUTF("Connexion réussie.");
                userAccount = new User(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"), rs.getString("password"));
                connectedUsers.put(email, dos); // Ajouter l'utilisateur à la Map des utilisateurs connectés
                return true;
            }
            dos.writeUTF("Échec de la connexion.");
            return false;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Vérifie si l'email est valide.
     *
     * @param email L'email à vérifier.
     * @return true si l'email est valide, false sinon.
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    /**
     * Vérifie si le mot de passe est valide.
     *
     * @param password Le mot de passe à vérifier.
     * @return true si le mot de passe est valide, false sinon.
     */
    private boolean isValidPassword(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*[0-9].*");
    }

    /**
     * Nettoie les ressources utilisées par le client.
     */
    private void cleanup() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (IOException | SQLException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }
}