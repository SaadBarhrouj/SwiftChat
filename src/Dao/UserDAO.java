package Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The UserDAO class provides database access methods for user-related operations.
 */
public class UserDAO {
    private Connection conn;

    /**
     * Constructor to initialize database connection.
     */
    public UserDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    /**
     * Validates the email format using a regex pattern.
     *
     * @param email The email to validate.
     * @return True if the email is valid, false otherwise.
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email != null && email.matches(emailRegex);
    }

    /**
     * Checks if a user with the given email already exists in the database.
     *
     * @param email The email to check.
     * @return True if the user exists, false otherwise.
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
     * Inserts a new user into the database after validating the email and confirming the password.
     *
     * @param name             The user's name.
     * @param email            The user's email.
     * @param password         The user's password.
     * @param confirmPassword   The confirmation password.
     * @return True if the insertion was successful, false otherwise.
     */
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

    /**
     * Retrieves a user from the database by email and password.
     *
     * @param email    The user's email.
     * @param password The user's password.
     * @return A ResultSet containing the user's data if found, null otherwise.
     */
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

    /**
     * Retrieves the user ID based on the provided email.
     *
     * @param email The user's email.
     * @return The user ID if found, -1 otherwise.
     */
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

    /**
     * Retrieves the email of a user based on their user ID.
     *
     * @param userId The user's ID.
     * @return The email associated with the user ID, or null if not found.
     */
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
