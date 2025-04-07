package Server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    private Connection conn;


    public UserDAO(Connection conn) {
        this.conn = conn;
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email != null && email.matches(emailRegex);
    }

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

    public boolean insertUser(String name, String email, String password, String confirmPassword) {
        if (!isValidEmail(email)) {
            System.out.println("Invalid email format.");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            System.out.println("Password and confirm password do not match.");
            return false;
        }

        if (userExists(email)) {
            System.out.println("User with this email already exists.");
            return false;
        }

        String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email.toLowerCase());
            pstmt.setString(3, password);
            return pstmt.executeUpdate() > 0; // Retourne true si l'insertion a réussi
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public ResultSet getUserByEmailAndPassword(String email, String password) {
        if (!isValidEmail(email)) {
            System.out.println("Invalid email format.");
            return null;
        }

        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email.toLowerCase());
            pstmt.setString(2, password);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


    public int getUserIdByEmail(String email) {
        String sql = "SELECT user_id FROM users WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("user_id") : -1; // Retourne l'ID si trouvé, -1 sinon
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public String getEmailById(int userId) {
        String sql = "SELECT email FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("email") : null; // Retourne l'email si trouvé, null sinon
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


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


    public void updateProfile(int userId, String newEmail, String newPassword, String newName) {
        updateEmail(userId, newEmail);
        updateName(userId, newName);
        updatePassword(userId, newPassword);
    }

    public boolean isUserOnline(int userId) {
        String sql = "SELECT is_online FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getBoolean("is_online");
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public void setUserOnlineStatus(int userId, boolean isOnline) {
        String sql = "UPDATE users SET is_online = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, isOnline);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}