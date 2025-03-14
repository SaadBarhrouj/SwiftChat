package Entities;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import Dao.DatabaseConnection;
import Dao.MessageDAO;
import Dao.UserDAO;
import Dao.GroupDAO;


public class User {
    private int id;
    private String name;
    private String email;
    private String password;
    private DataOutputStream dos;
    private DataInputStream dis;
    private Lock lock = new ReentrantLock();
    private static Map<String, DataOutputStream> mapDos = new HashMap<>();

    private Connection conn;
    private MessageDAO messageDAO;
    private UserDAO userDAO;
    private GroupDAO groupDAO;
    private static ArrayList<Group> allGroups = new ArrayList<>();
    private ArrayList<Group> myGroups = new ArrayList<>();


    public User(int id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.conn = DatabaseConnection.getConnection();
        this.messageDAO = new MessageDAO();
        this.userDAO = new UserDAO();
        this.groupDAO = new GroupDAO();
    }

   /* public boolean sendMessage(String target, String msg) {
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
    }*/

    /**
     * Envoie un fichier à un destinataire.
     *
     * @param target    L'email du destinataire
     * @param fileName  Le nom du fichier
     * @param fileData  Les données du fichier
     * @return true si le fichier a été envoyé avec succès, false sinon
     */
   /* public boolean sendFile(String target, String fileName, byte[] fileData) {
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
    }*/

    /**
     * Déconnecte l'utilisateur et le retire de la liste des utilisateurs en ligne.
     */
    public void disconnect() {
        mapDos.remove(this.email);
        System.out.println(this.getEmail() + " déconnecté");
    }


    public void addGroup(Group group) {
        myGroups.add(group);
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void updateName(String newName) {
        this.name = newName;
        userDAO.updateName(this.id, newName);
    }

    public void updateEmail(String newEmail) {
        this.email = newEmail.toLowerCase();
        userDAO.updateEmail(this.id, newEmail);
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
        userDAO.updatePassword(this.id, newPassword);
    }

    public void updateProfile(String newEmail, String newPassword, String newName) {
        updateEmail(newEmail);
        updateName(newName);
        updatePassword(newPassword);
        System.out.println("Profil mis à jour avec succès !");
    }


}