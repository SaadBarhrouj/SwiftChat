package Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    private Connection conn;

    public UserDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    /**
     * Vérifie si un utilisateur existe dans la base de données.
     *
     * @param email L'email de l'utilisateur.
     * @return true si l'utilisateur existe, sinon false.
     */
    public boolean userExists(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Retourne true si un utilisateur avec cet email existe
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Insère un nouvel utilisateur dans la base de données.
     *
     * @param name     Le nom de l'utilisateur.
     * @param email    L'email de l'utilisateur.
     * @param password Le mot de passe de l'utilisateur.
     * @return true si l'insertion a réussi, sinon false.
     */
    public boolean insertUser(String name, String email, String password) {
        String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            return pstmt.executeUpdate() > 0; // Retourne true si l'insertion a réussi
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Récupère un utilisateur par son email et son mot de passe.
     *
     * @param email    L'email de l'utilisateur.
     * @param password Le mot de passe de l'utilisateur.
     * @return Un ResultSet contenant les informations de l'utilisateur, ou null si non trouvé.
     */
    public ResultSet getUserByEmailAndPassword(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Met à jour le nom d'un utilisateur dans la base de données.
     *
     * @param userId  L'ID de l'utilisateur.
     * @param newName Le nouveau nom.
     */
    public void updateName(int userId, String newName) {
        try {
            String sql = "UPDATE users SET name = ? WHERE user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newName);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Met à jour l'email d'un utilisateur dans la base de données.
     *
     * @param userId   L'ID de l'utilisateur.
     * @param newEmail Le nouvel email.
     */
    public void updateEmail(int userId, String newEmail) {
        try {
            String sql = "UPDATE users SET email = ? WHERE user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newEmail.toLowerCase());
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Met à jour le mot de passe d'un utilisateur dans la base de données.
     *
     * @param userId      L'ID de l'utilisateur.
     * @param newPassword Le nouveau mot de passe.
     */
    public void updatePassword(int userId, String newPassword) {
        try {
            String sql = "UPDATE users SET password = ? WHERE user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newPassword);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Met à jour le profil complet d'un utilisateur (nom, email, mot de passe).
     *
     * @param userId      L'ID de l'utilisateur.
     * @param newEmail    Le nouvel email.
     * @param newPassword Le nouveau mot de passe.
     * @param newName     Le nouveau nom.
     */
    public void updateProfile(int userId, String newEmail, String newPassword, String newName) {
        updateEmail(userId, newEmail);
        updateName(userId, newName);
        updatePassword(userId, newPassword);
    }
}