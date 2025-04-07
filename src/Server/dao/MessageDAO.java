package Server.dao;

import Server.entities.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    private Connection conn;
    private UserDAO userDAO;

    public MessageDAO(Connection conn) {
        this.conn = conn;
        this.userDAO = new UserDAO(this.conn);
    }

    // Insert TEXT message (private) - OK
    public int insertMessage(int senderId, int receiverId, String message) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, message, messageType, date) VALUES (?, ?, ?, 'text', NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setString(3, message);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur insertion message privé: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    // Insert FILE message (private) - OK
    public int insertFileMessage(int senderId, int receiverId, String fileName) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, message, messageType, fileName, date) VALUES (?, ?, NULL, 'file', ?, NOW())"; // message est NULL pour file
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setString(3, fileName);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur insertion fichier privé: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    // Store Pending Message - OK (generic)
    public boolean storePendingMessage(int userId, int messageId) {
        // Vérifier si la combinaison existe déjà pour éviter les doublons
        String checkSql = "SELECT COUNT(*) FROM pending_messages WHERE user_id = ? AND message_id = ?";
        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setInt(1, userId);
            checkPs.setInt(2, messageId);
            ResultSet checkRs = checkPs.executeQuery();
            if (checkRs.next() && checkRs.getInt(1) > 0) {
                // System.out.println("Message " + messageId + " déjà en attente pour user " + userId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur vérification message en attente: " + e.getMessage());
            // Continuer pour tenter l'insertion malgré l'échec de la vérification
        }

        String sql = "INSERT INTO pending_messages (user_id, message_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, messageId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Gérer les cas où l'insertion échoue (par exemple, violation de contrainte si la vérification a échoué mais la donnée existe)
            if (e.getErrorCode() == 1062) { // Code d'erreur MySQL pour Duplicate entry
                return true; // Considérer comme succès si c'est un doublon
            }
            System.err.println("Erreur stockage message en attente: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }


    // Get Pending Messages - OK (generic, récupère déjà type/filename)
    public ArrayList<Message> getPendingMessagesForUser(int userId) {
        ArrayList<Message> messages = new ArrayList<>();
        // ===== REQUÊTE SQL MODIFIÉE (ajout de m.messageType, m.fileName, m.group_id) =====
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
                        rs.getInt("sender_id"),
                        rs.getString("senderEmail"),
                        // receiver_id n'est pas directement utile ici, mais on pourrait le récupérer si besoin
                        userDAO.getEmailById(rs.getInt("receiver_id")), // Peut être null pour les groupes
                        rs.getString("message"), // Sera null si type='file'
                        rs.getString("messageType"), // Récupéré
                        rs.getString("fileName"),    // Récupéré
                        rs.getString("date"),
                        rs.getInt("group_id")       // Récupéré
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération messages en attente: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }


    // Delete Pending Messages - OK (generic)
    public void deletePendingMessagesForUser(int userId) {
        String sql = "DELETE FROM pending_messages WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur suppression messages en attente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Get Private Conversation - OK (récupère déjà type/filename)
    public List<Message> getConversation(int userId1, int userId2) {
        List<Message> messages = new ArrayList<>();
        // ===== REQUÊTE SQL OK (récupère déjà tous les champs nécessaires pour Message) =====
        String sql = "SELECT m.*, u1.email AS senderEmail, u2.email AS receiverEmail " +
                "FROM messages m " +
                "JOIN users u1 ON m.sender_id = u1.user_id " +
                "JOIN users u2 ON m.receiver_id = u2.user_id " +
                "WHERE m.group_id IS NULL AND ((m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?)) " + // Ajout m.group_id IS NULL
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
                        rs.getInt("sender_id"),
                        rs.getString("senderEmail"),
                        rs.getString("receiverEmail"),
                        rs.getString("message"),
                        rs.getString("messageType"),
                        rs.getString("fileName"),
                        rs.getString("date"),
                        0 // groupId est 0 ou null pour les messages privés
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération conversation: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }


    // Get Group Messages - MODIFIED
    public List<Message> getGroupMessages(int groupId) {
        List<Message> messages = new ArrayList<>();
        // ===== MODIFIÉ : Ajout de m.messageType et m.fileName =====
        String query = "SELECT m.message_id, m.sender_id, m.group_id, m.message, m.messageType, m.fileName, m.date, u.email AS sender_email " +
                "FROM messages m " +
                "JOIN users u ON m.sender_id = u.user_id " +
                "WHERE m.group_id = ? " +
                "ORDER BY m.date ASC";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                // ===== MODIFIÉ : Utilisation des champs récupérés =====
                Message message = new Message(
                        rs.getInt("message_id"),
                        rs.getInt("sender_id"),
                        rs.getString("sender_email"),
                        null, // receiverEmail est null pour les groupes
                        rs.getString("message"),       // Sera null si type='file'
                        rs.getString("messageType"),   // <-- Utilise la valeur BDD
                        rs.getString("fileName"),      // <-- Utilise la valeur BDD
                        rs.getString("date"),
                        groupId // Utilise le groupId passé en paramètre (ou rs.getInt("group_id"))
                );
                messages.add(message);
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération messages groupe: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }


    // Get Message By ID - OK (récupère déjà tous les champs)
    public Message getMessageById(int messageId) {
        String sql = "SELECT m.*, u_sender.email AS senderEmail, u_receiver.email AS receiverEmail " +
                "FROM messages m " +
                "LEFT JOIN users u_sender ON m.sender_id = u_sender.user_id " +
                "LEFT JOIN users u_receiver ON m.receiver_id = u_receiver.user_id " + // Jointure pour l'email du destinataire (peut être NULL)
                "WHERE m.message_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Message(
                        rs.getInt("message_id"),
                        rs.getInt("sender_id"),
                        rs.getString("senderEmail"),
                        rs.getString("receiverEmail"), // Sera null si group_id n'est pas null
                        rs.getString("message"),
                        rs.getString("messageType"),
                        rs.getString("fileName"),
                        rs.getString("date"),
                        rs.getInt("group_id") // Récupération du group_id depuis la BDD
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du message par ID (" + messageId + "): " + e.getMessage());
            e.printStackTrace();
        }
        return null; // Non trouvé ou erreur
    }

    // Insert TEXT message (group) - OK
    public int insertGroupMessage(int senderId, int groupId, String message) {
        String query = "INSERT INTO messages (sender_id, group_id, message, messageType, date) VALUES (?, ?, ?, 'text', NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, senderId);
            stmt.setInt(2, groupId);
            stmt.setString(3, message);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création du message groupe a échoué, aucune ligne affectée.");
            }
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("La création du message groupe a échoué, aucun ID obtenu.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur insertion message groupe: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }


    // Insert FILE message (group) - OK
    public int insertGroupFileMessage(int senderId, int groupId, String fileName) {
        String sql = "INSERT INTO messages (sender_id, group_id, message, messageType, fileName, date) VALUES (?, ?, NULL, 'file', ?, NOW())"; // message est NULL pour file
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, senderId);
            ps.setInt(2, groupId);
            ps.setString(3, fileName);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur insertion fichier groupe: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    // Get Group Files - OK (méthode dédiée)
    public List<Message> getGroupFiles(int groupId) {
        List<Message> files = new ArrayList<>();
        // Requête récupère bien les infos nécessaires
        String query = "SELECT m.message_id, m.sender_id, m.group_id, m.fileName, m.date, u.email AS sender_email " +
                "FROM messages m " +
                "JOIN users u ON m.sender_id = u.user_id " +
                "WHERE m.group_id = ? AND m.messageType = 'file' " +
                "ORDER BY m.date DESC";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Message file = new Message(
                        rs.getInt("message_id"),
                        rs.getInt("sender_id"),
                        rs.getString("sender_email"),
                        null, // receiverEmail
                        null, // message
                        "file", // messageType
                        rs.getString("fileName"),
                        rs.getString("date"),
                        groupId // ou rs.getInt("group_id")
                );
                files.add(file);
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération fichiers groupe: " + e.getMessage());
            e.printStackTrace();
        }
        return files;
    }




    public boolean markMessageAsDeleted(int messageId, int requestingUserId) {
        String fetchSenderSql = "SELECT sender_id FROM messages WHERE message_id = ?";
        String updateSql = "UPDATE messages SET message = '[Message supprimé]', messageType = 'deleted', fileName = NULL WHERE message_id = ? AND sender_id = ?";

        PreparedStatement pstmtFetch = null;
        PreparedStatement pstmtUpdate = null;
        ResultSet rs = null;
        boolean success = false;

        if (this.conn == null) {
            System.err.println("[DAO ERROR] La connexion est null dans markMessageAsDeleted!");
            return false;
        }

        try {
            // 1. Vérifier qui est l'expéditeur original du message
            pstmtFetch = this.conn.prepareStatement(fetchSenderSql);
            pstmtFetch.setInt(1, messageId);
            rs = pstmtFetch.executeQuery();

            if (rs.next()) {
                int originalSenderId = rs.getInt("sender_id");

                // 2. Vérifier si l'utilisateur qui demande a le droit (est l'expéditeur)
                if (originalSenderId == requestingUserId) {

                    // 3. Si oui, exécuter la mise à jour pour marquer comme supprimé
                    pstmtUpdate = this.conn.prepareStatement(updateSql);
                    pstmtUpdate.setInt(1, messageId);
                    pstmtUpdate.setInt(2, requestingUserId); // Sécurité supplémentaire

                    int rowsAffected = pstmtUpdate.executeUpdate();
                    if (rowsAffected > 0) {
                        System.out.println("[DAO] Message ID " + messageId + " marqué comme supprimé par user ID " + requestingUserId);
                        success = true; // La mise à jour a réussi
                    } else {
                        System.err.println("[DAO] Échec de la mise à jour pour supprimer msg ID " + messageId + " (peut-être déjà supprimé ou problème interne).");
                    }
                } else {
                    // Pas la permission
                    System.err.println("[DAO] Permission refusée: User ID " + requestingUserId + " ne peut pas supprimer msg ID " + messageId + " (envoyé par " + originalSenderId + ")");
                }
            } else {
                // Le message n'a pas été trouvé
                System.err.println("[DAO] Message ID " + messageId + " non trouvé pour suppression.");
            }

        } catch (SQLException e) {
            System.err.println("SQL Error lors de la suppression du message " + messageId + ": " + e.getMessage());
            e.printStackTrace();
            success = false; // Assurer false en cas d'erreur SQL
        } finally {
            // Fermer PreparedStatement et ResultSet proprement
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignorer */ }
            try { if (pstmtFetch != null) pstmtFetch.close(); } catch (SQLException e) { /* ignorer */ }
            try { if (pstmtUpdate != null) pstmtUpdate.close(); } catch (SQLException e) { /* ignorer */ }
            // NE PAS fermer this.conn ici, elle est gérée par ClientHandler
        }
        return success;
    }


    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }
}