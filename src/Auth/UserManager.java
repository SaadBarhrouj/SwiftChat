package Auth;

import Database.DatabaseConnection;
import java.sql.*;
import java.io.*;

public class UserManager {
    private final DatabaseConnection db;
    private final DataOutputStream dos;

    public UserManager(DatabaseConnection db, DataOutputStream dos) {
        this.db = db;
        this.dos = dos;
    }

    public boolean registerUser(String name, String email, String password) throws IOException {
        try {
            String sql = "SELECT * FROM users WHERE email = ?";
            PreparedStatement pstmt = db.getConnection().prepareStatement(sql);
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                dos.writeUTF("Échec de l'inscription. L'email est déjà utilisé.");
                return false;
            } else {
                sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
                pstmt = db.getConnection().prepareStatement(sql);
                pstmt.setString(1, name);
                pstmt.setString(2, email);
                pstmt.setString(3, password);
                pstmt.executeUpdate();
                dos.writeUTF("Inscription réussie. Vous pouvez maintenant vous connecter.");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean authenticateUser(String email, String password) throws IOException {
        try {
            String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
            PreparedStatement pstmt = db.getConnection().prepareStatement(sql);
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                dos.writeUTF("Connexion réussie. Bienvenue, " + rs.getString("name") + "!");
                return true;
            } else {
                dos.writeUTF("Échec de la connexion. Vérifiez vos informations et réessayez.");
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
