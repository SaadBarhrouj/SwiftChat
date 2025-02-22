package Database;

import Client.UserAccount;

import java.sql.*;
import java.util.ArrayList;

public class UserDAO {
    private Connection conn;
    private Statement stmt;
    private ResultSet rs;

    public UserDAO() {
        this.conn = DatabaseConnection.getConnection();
        if (this.conn != null) {
            try {
                this.stmt = this.conn.createStatement();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Méthode pour vérifier si l'email est déjà utilisé
    public boolean userExists(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            rs = ps.executeQuery();
            return rs.next(); // Si un utilisateur avec cet email existe
        }
    }

    // Méthode pour insérer un nouvel utilisateur
    public boolean insertUser(String name, String email, String password) throws SQLException {
        String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, password);
            return ps.executeUpdate() > 0; // Retourne true si l'insertion a réussi
        }
    }


}
