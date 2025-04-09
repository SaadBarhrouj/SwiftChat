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
            System.err.println("Error inserting private message: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public int insertFileMessage(int senderId, int receiverId, String fileName) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, message, messageType, fileName, date) VALUES (?, ?, NULL, 'file', ?, NOW())"; // message is NULL for files
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
            System.err.println("Error inserting private file: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public boolean storePendingMessage(int userId, int messageId) {
        String checkSql = "SELECT COUNT(*) FROM pending_messages WHERE user_id = ? AND message_id = ?";
        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setInt(1, userId);
            checkPs.setInt(2, messageId);
            ResultSet checkRs = checkPs.executeQuery();
            if (checkRs.next() && checkRs.getInt(1) > 0) {

                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error checking pending message: " + e.getMessage());

        }

        String sql = "INSERT INTO pending_messages (user_id, message_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, messageId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {

            if (e.getErrorCode() == 1062) {
                return true;
            }
            System.err.println("Error storing pending message: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }


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
                        rs.getInt("sender_id"),
                        rs.getString("senderEmail"),

                        userDAO.getEmailById(rs.getInt("receiver_id")),
                        rs.getString("message"),
                        rs.getString("messageType"),
                        rs.getString("fileName"),
                        rs.getString("date"),
                        rs.getInt("group_id")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving pending messages: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }


    public void deletePendingMessagesForUser(int userId) {
        String sql = "DELETE FROM pending_messages WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting pending messages: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public List<Message> getConversation(int userId1, int userId2) {
        List<Message> messages = new ArrayList<>();

        String sql = "SELECT m.*, u1.email AS senderEmail, u2.email AS receiverEmail " +
                "FROM messages m " +
                "JOIN users u1 ON m.sender_id = u1.user_id " +
                "JOIN users u2 ON m.receiver_id = u2.user_id " +
                "WHERE m.group_id IS NULL AND ((m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?)) " + // Added m.group_id IS NULL
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
                        0
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving conversation: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }


    public List<Message> getGroupMessages(int groupId) {
        List<Message> messages = new ArrayList<>();

        String query = "SELECT m.message_id, m.sender_id, m.group_id, m.message, m.messageType, m.fileName, m.date, u.email AS sender_email " +
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
                        rs.getInt("sender_id"),
                        rs.getString("sender_email"),
                        null,
                        rs.getString("message"),
                        rs.getString("messageType"),
                        rs.getString("fileName"),
                        rs.getString("date"),
                        groupId
                );
                messages.add(message);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving group messages: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }


    public Message getMessageById(int messageId) {
        String sql = "SELECT m.*, u_sender.email AS senderEmail, u_receiver.email AS receiverEmail " +
                "FROM messages m " +
                "LEFT JOIN users u_sender ON m.sender_id = u_sender.user_id " +
                "LEFT JOIN users u_receiver ON m.receiver_id = u_receiver.user_id " + // Join for recipient email (can be NULL)
                "WHERE m.message_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Message(
                        rs.getInt("message_id"),
                        rs.getInt("sender_id"),
                        rs.getString("senderEmail"),
                        rs.getString("receiverEmail"),
                        rs.getString("message"),
                        rs.getString("messageType"),
                        rs.getString("fileName"),
                        rs.getString("date"),
                        rs.getInt("group_id")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving message by ID (" + messageId + "): " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public int insertGroupMessage(int senderId, int groupId, String message) {
        String query = "INSERT INTO messages (sender_id, group_id, message, messageType, date) VALUES (?, ?, ?, 'text', NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, senderId);
            stmt.setInt(2, groupId);
            stmt.setString(3, message);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating group message failed, no rows affected.");
            }
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating group message failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting group message: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }


    public int insertGroupFileMessage(int senderId, int groupId, String fileName) {
        String sql = "INSERT INTO messages (sender_id, group_id, message, messageType, fileName, date) VALUES (?, ?, NULL, 'file', ?, NOW())"; // message is NULL for files
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
            System.err.println("Error inserting group file: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }



    public boolean markMessageAsDeleted(int messageId, int requestingUserId) {
        String fetchSenderSql = "SELECT sender_id FROM messages WHERE message_id = ?";
        String updateSql = "UPDATE messages SET message = '[Message deleted]', messageType = 'deleted', fileName = NULL WHERE message_id = ? AND sender_id = ?";

        PreparedStatement pstmtFetch = null;
        PreparedStatement pstmtUpdate = null;
        ResultSet rs = null;
        boolean success = false;

        if (this.conn == null) {
            System.err.println("[DAO ERROR] Connection is null in markMessageAsDeleted!");
            return false;
        }

        try {

            pstmtFetch = this.conn.prepareStatement(fetchSenderSql);
            pstmtFetch.setInt(1, messageId);
            rs = pstmtFetch.executeQuery();

            if (rs.next()) {
                int originalSenderId = rs.getInt("sender_id");


                if (originalSenderId == requestingUserId) {
                    pstmtUpdate = this.conn.prepareStatement(updateSql);
                    pstmtUpdate.setInt(1, messageId);
                    pstmtUpdate.setInt(2, requestingUserId); // Additional security

                    int rowsAffected = pstmtUpdate.executeUpdate();
                    if (rowsAffected > 0) {
                        System.out.println("[DAO] Message ID " + messageId + " marked as deleted by user ID " + requestingUserId);
                        success = true;
                    } else {
                        System.err.println("[DAO] Failed to update message ID " + messageId + " (may already be deleted or internal issue).");
                    }
                } else {

                    System.err.println("[DAO] Permission denied: User ID " + requestingUserId + " cannot delete message ID " + messageId + " (sent by " + originalSenderId + ")");
                }
            } else {

                System.err.println("[DAO] Message ID " + messageId + " not found for deletion.");
            }

        } catch (SQLException e) {
            System.err.println("SQL Error while deleting message " + messageId + ": " + e.getMessage());
            e.printStackTrace();
            success = false;
        } finally {

            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (pstmtFetch != null) pstmtFetch.close(); } catch (SQLException e) { }
            try { if (pstmtUpdate != null) pstmtUpdate.close(); } catch (SQLException e) {  }

        }
        return success;
    }

}