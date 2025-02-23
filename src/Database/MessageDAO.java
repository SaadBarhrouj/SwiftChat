package Database;

import Entities.Message;
import java.sql.*;
import java.util.ArrayList;

public class MessageDAO {
    private Connection conn;

    public MessageDAO() {
        this.conn = DatabaseConnection.getConnection();
    }



    public ArrayList<Message> getMessagesForUser(String email) {
        ArrayList<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE receiver_Email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                messages.add(new Message(
                        rs.getInt("message_id"),
                        rs.getString("sender_Email"),
                        rs.getString("receiver_Email"),
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

    public void deleteMessagesForUser(String email) {
        String sql = "DELETE FROM messages WHERE receiver_Email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertMessage(String senderEmail, String receiverEmail, String message, String messageType, String fileName, Timestamp date) {
        String sql = "INSERT INTO messages (sender_Email, receiver_Email, message, messageType, fileName, date) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, senderEmail);
            ps.setString(2, receiverEmail);
            ps.setString(3, message);
            ps.setString(4, messageType);
            ps.setString(5, fileName);
            ps.setTimestamp(6, date); // Utiliser setTimestamp pour la date
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}