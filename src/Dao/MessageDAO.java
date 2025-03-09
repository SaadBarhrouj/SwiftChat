package Dao;

import Entities.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    private Connection conn;

    public MessageDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    /**
     * Insérer un message dans la table `messages`.
     *
     * @param senderId   ID de l'expéditeur.
     * @param receiverId ID du destinataire.
     * @param message    Contenu du message.
     * @return L'ID du message inséré, ou -1 en cas d'échec.
     */
    public int insertMessage(int senderId, int receiverId, String message) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, message, messageType, date) VALUES (?, ?, ?, 'text', NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setString(3, message);
            ps.executeUpdate();

            // Récupérer l'ID du message inséré
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Retourne -1 en cas d'échec
    }
    public int insertFileMessage(int senderId, int receiverId, String fileName) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, message, messageType, fileName, date) VALUES (?, ?, '', 'file', ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setString(3, fileName);
            ps.executeUpdate();
    
            // Récupérer l'ID du message inséré
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Retourne -1 en cas d'échec
    }
    /**
     * Stocker un message en attente dans la table `pending_messages`.
     *
     * @param userId    ID de l'utilisateur destinataire.
     * @param messageId ID du message à stocker.
     * @return true si l'insertion a réussi, sinon false.
     */
    public boolean storePendingMessage(int userId, int messageId) {
        String sql = "INSERT INTO pending_messages (user_id, message_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, messageId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Récupérer les messages en attente pour un utilisateur.
     *
     * @param userId ID de l'utilisateur.
     * @return Liste des messages en attente.
     */
    public ArrayList<Message> getPendingMessagesForUser(int userId) {
        ArrayList<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, u.email AS senderEmail " +
                "FROM pending_messages pm " +
                "JOIN messages m ON pm.message_id = m.message_id " +
                "JOIN users u ON m.sender_id = u.user_id " +
                "WHERE pm.user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                messages.add(new Message(
                        rs.getInt("message_id"),
                        rs.getString("senderEmail"),
                        rs.getString("receiverEmail"),
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
     * Supprimer les messages en attente pour un utilisateur.
     *
     * @param userId ID de l'utilisateur.
     */
    public void deletePendingMessagesForUser(int userId) {
        String sql = "DELETE FROM pending_messages WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Récupérer la conversation entre deux utilisateurs.
     *
     * @param userId1 ID du premier utilisateur.
     * @param userId2 ID du deuxième utilisateur.
     * @return Liste des messages de la conversation.
     */
    public List<Message> getConversation(int userId1, int userId2) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, u1.email AS senderEmail, u2.email AS receiverEmail " +
                "FROM messages m " +
                "JOIN users u1 ON m.sender_id = u1.user_id " +
                "JOIN users u2 ON m.receiver_id = u2.user_id " +
                "WHERE (m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?) " +
                "ORDER BY m.date ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId1);
            ps.setInt(2, userId2);
            ps.setInt(3, userId2);
            ps.setInt(4, userId1);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                messages.add(new Message(
                        rs.getInt("message_id"),
                        rs.getString("senderEmail"),
                        rs.getString("receiverEmail"),
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
}