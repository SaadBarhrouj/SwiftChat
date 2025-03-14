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
                        null, // receiverEmail n'est pas récupéré dans la requête
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

    public List<Message> getGroupMessages(int groupId) {
        List<Message> messages = new ArrayList<>();
        String query = "SELECT m.message_id, m.sender_id, m.group_id, m.message, m.date, u.email AS sender_email " +
                "FROM messages m " +
                "JOIN users u ON m.sender_id = u.user_id " +
                "WHERE m.group_id = ? " +
                "ORDER BY m.date ASC";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Message message = new Message(
                        rs.getInt("message_id"),
                        rs.getString("sender_email"),
                        null, // receiverEmail is not applicable for group messages
                        rs.getString("message"),
                        "text", // Assuming messageType is 'text' for group messages
                        null, // fileName is not applicable for text messages
                        rs.getString("date")
                );
                messages.add(message);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
    public int insertGroupMessage(int senderId, int groupId, String message) {
        String query = "INSERT INTO messages (sender_id, group_id, message, date) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, senderId);
            stmt.setInt(2, groupId);
            stmt.setString(3, message);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création du message a échoué, aucune ligne affectée.");
            }
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("La création du message a échoué, aucun ID obtenu.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }
}