package Database;

import com.mysql.cj.protocol.Message;

import java.sql.*;
import java.util.ArrayList;

public class MessageDAO {
    private Connection conn;
    private Statement stmt;
    private ResultSet rs;
    public MessageDAO() {
        this.conn = DatabaseConnection.getConnection();
        if (this.conn != null) {
            try {
                this.stmt = this.conn.createStatement();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Méthode pour récupérer un utilisateur par son email et mot de passe
    public ResultSet getUserByEmailAndPassword(String email, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, email);
        ps.setString(2, password);
        return ps.executeQuery(); // Retourne le résultat de la requête
    }

    // Méthode pour récupérer tous les messages d'un utilisateur
    public ArrayList<message> getMessagesForUser(String email) throws SQLException {
        String sql = "SELECT * FROM messages WHERE receiver_Email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            rs = ps.executeQuery();

            ArrayList<message> messages = new ArrayList<>();
            while (rs.next()) {
                String messageType = rs.getString("messageType");
                String senderEmail = rs.getString("sender_Email");
                String date = rs.getString("date");
                String content = rs.getString("message");
                String fileName = rs.getString("fileName");

                messages.add(new Message(messageType, senderEmail, date, content, fileName));
            }
            return messages;
        }
    }

    // Méthode pour supprimer les messages d'un utilisateur après les avoir reçus
    public void deleteMessagesForUser(String email) throws SQLException {
        String sql = "DELETE FROM messages WHERE receiver_Email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        }
    }
}
