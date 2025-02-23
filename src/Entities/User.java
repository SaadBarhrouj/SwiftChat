package Entities;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import Database.DatabaseConnection;
import Database.MessageDAO;
import java.util.HashMap;
import java.util.Map;

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
    private MessageDAO messageDAO; // Ajout de l'instance de MessageDAO

    public User(int id, String email, String name, DataOutputStream dos, DataInputStream dis) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.dos = dos;
        this.dis = dis;
        this.conn = DatabaseConnection.getConnection();
        this.messageDAO = new MessageDAO(); // Initialisation de MessageDAO
        mapDos.put(email, dos); // Ajouter au map des utilisateurs en ligne
    }

    public void lockMe() {
        lock.lock();
    }

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
            // Convertir le timestamp en une date MySQL valide
            Timestamp sqlTimestamp = new Timestamp(System.currentTimeMillis());
            messageDAO.insertMessage(this.getEmail(), target, encodedFileData, "file", fileName, sqlTimestamp);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void disconnect() {
        mapDos.remove(this.email); // Retirer l'utilisateur de la liste des utilisateurs en ligne
        System.out.println(this.getEmail() + " déconnecté");
    }
}