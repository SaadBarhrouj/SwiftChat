package Dao;

import Entities.Message;
import java.sql.*;
import java.util.ArrayList;

/**
 * La classe MessageDAO fournit des méthodes d'accès à la base de données pour les opérations liées aux messages.
 */
public class MessageDAO {
    private Connection conn;

    /**
     * Constructeur pour initialiser la connexion à la base de données.
     */
    public MessageDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    /**
     * Récupérer les messages pour un utilisateur (via son email).
     *
     * @param email L'email de l'utilisateur.
     * @return Une liste de messages pour l'utilisateur.
     */
    public ArrayList<Message> getMessagesForUser(String email) {
        ArrayList<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, u1.email AS senderEmail, u2.email AS receiverEmail " +
                "FROM messages m " +
                "JOIN users u1 ON m.sender_id = u1.user_id " +
                "JOIN users u2 ON m.receiver_id = u2.user_id " +
                "WHERE m.receiver_Email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                messages.add(new Message(
                        rs.getInt("message_id"),
                        rs.getString("senderEmail"), // Utiliser l'email de l'expéditeur
                        rs.getString("receiverEmail"), // Utiliser l'email du destinataire
                        rs.getString("message"),
                        rs.getString("messageType"),
                        rs.getString("fileName"),
                        rs.getString("date")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    /**
     * Supprimer les messages pour un utilisateur (via son email).
     *
     * @param email L'email de l'utilisateur.
     */
    public void deleteMessagesForUser(String email) {
        String sql = "DELETE FROM messages WHERE receiver_Email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Insérer un nouveau message.
     *
     * @param senderEmail   L'email de l'expéditeur.
     * @param receiverEmail L'email du destinataire.
     * @param message       Le contenu du message.
     * @param messageType   Le type de message (texte, fichier, etc.).
     * @param fileName      Le nom du fichier (si applicable).
     * @param date          La date d'envoi du message.
     */
    public void insertMessage(String senderEmail, String receiverEmail, String message, String messageType, String fileName, Timestamp date) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, sender_Email, receiver_Email, message, messageType, fileName, date) " +
                "VALUES ((SELECT user_id FROM users WHERE email = ?), (SELECT user_id FROM users WHERE email = ?), ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, senderEmail); // sender_id
            ps.setString(2, receiverEmail); // receiver_id
            ps.setString(3, senderEmail); // sender_Email
            ps.setString(4, receiverEmail); // receiver_Email
            ps.setString(5, message);
            ps.setString(6, messageType);
            ps.setString(7, fileName);
            ps.setTimestamp(8, date);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
