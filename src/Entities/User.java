package Entities;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import Dao.DatabaseConnection;
import Dao.MessageDAO;
import Dao.UserDAO;

public class User {
    private int id;
    private String name;
    private String email;
    private String password;
    private DataOutputStream dos;
    private DataInputStream dis;
    private Lock lock = new ReentrantLock();
    private static Map<String, DataOutputStream> mapDos = new HashMap<>(); // Stocker les streams des utilisateurs en ligne

    private Connection conn;
    private MessageDAO messageDAO; // Pour gérer les messages
    private UserDAO userDAO; // Pour gérer les mises à jour du profil

    public User(int id, String email, String name, DataOutputStream dos, DataInputStream dis) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.dos = dos;
        this.dis = dis;
        this.conn = DatabaseConnection.getConnection();
        this.messageDAO = new MessageDAO(); // Initialisation de MessageDAO
        this.userDAO = new UserDAO(); // Initialisation de UserDAO
        mapDos.put(email, dos); // Ajouter au map des utilisateurs en ligne
    }

    /**
     * Verrouille l'utilisateur pour les opérations thread-safe.
     */
    public void lockMe() {
        lock.lock();
    }

    /**
     * Déverrouille l'utilisateur.
     */
    public void unlockMe() {
        lock.unlock();
    }

    /**
     * Envoie un message à un destinataire.
     *
     * @param target Email du destinataire.
     * @param msg    Contenu du message.
     * @return true si le message a été envoyé avec succès, sinon false.
     */
    public boolean sendMessage(String target, String msg) {
        System.out.println(this.getEmail() + " envoie un message à " + target);

        // Vérifier si le destinataire est en ligne
        if (mapDos.containsKey(target)) {
            try {
                DataOutputStream targetDos = mapDos.get(target);
                targetDos.writeUTF("Nouveau message de " + this.getEmail() + " : " + msg); // Envoyer le message en clair
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Échec d'envoi du message");
                return false;
            }
        }

        // Si le destinataire n'est pas en ligne, sauvegarder le message dans la base de données
        try {
            Timestamp sqlTimestamp = new Timestamp(System.currentTimeMillis());
            messageDAO.insertMessage(this.getEmail(), target, msg, "text", null, sqlTimestamp);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Envoie un fichier (image, vidéo, etc.) à un destinataire.
     *
     * @param target   Email du destinataire.
     * @param fileName Nom du fichier.
     * @param fileData Données du fichier.
     * @return true si le fichier a été envoyé avec succès, sinon false.
     */
    public boolean sendFile(String target, String fileName, byte[] fileData) {
        System.out.println(this.getEmail() + " envoie un fichier à " + target);

        // Vérifier si le destinataire est en ligne
        if (mapDos.containsKey(target)) {
            try {
                DataOutputStream targetDos = mapDos.get(target);
                targetDos.writeUTF("file:" + fileName); // Envoyer le type de message et le nom du fichier
                targetDos.writeInt(fileData.length); // Envoyer la taille des données
                targetDos.write(fileData); // Envoyer les données du fichier
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Échec d'envoi du fichier");
                return false;
            }
        }

        // Si le destinataire n'est pas en ligne, sauvegarder le fichier dans la base de données
        try {
            String encodedFileData = Base64.getEncoder().encodeToString(fileData); // Encoder les données en Base64
            Timestamp sqlTimestamp = new Timestamp(System.currentTimeMillis());
            messageDAO.insertMessage(this.getEmail(), target, encodedFileData, "file", fileName, sqlTimestamp);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Met à jour le nom de l'utilisateur.
     *
     * @param newName Le nouveau nom.
     */
    public void updateName(String newName) {
        this.name = newName;
        userDAO.updateName(this.id, newName); // Appel à UserDAO
    }

    /**
     * Met à jour l'email de l'utilisateur.
     *
     * @param newEmail Le nouvel email.
     */
    public void updateEmail(String newEmail) {
        this.email = newEmail.toLowerCase();
        userDAO.updateEmail(this.id, newEmail); // Appel à UserDAO
    }

    /**
     * Met à jour le mot de passe de l'utilisateur.
     *
     * @param newPassword Le nouveau mot de passe.
     */
    public void updatePassword(String newPassword) {
        this.password = newPassword;
        userDAO.updatePassword(this.id, newPassword); // Appel à UserDAO
    }

    /**
     * Met à jour le profil complet de l'utilisateur (nom, email, mot de passe).
     *
     * @param newEmail    Le nouvel email.
     * @param newPassword Le nouveau mot de passe.
     * @param newName     Le nouveau nom.
     */
    public void updateProfile(String newEmail, String newPassword, String newName) {
        updateEmail(newEmail);
        updateName(newName);
        updatePassword(newPassword);
        System.out.println("Profil mis à jour avec succès !");
    }

    /**
     * Récupère l'ID de l'utilisateur.
     *
     * @return L'ID de l'utilisateur.
     */
    public int getId() {
        return id;
    }

    /**
     * Récupère l'email de l'utilisateur.
     *
     * @return L'email de l'utilisateur.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Déconnecte l'utilisateur.
     */
    public void disconnect() {
        mapDos.remove(this.email); // Retirer l'utilisateur de la liste des utilisateurs en ligne
        System.out.println(this.getEmail() + " déconnecté");
    }
}