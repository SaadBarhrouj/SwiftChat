package Dao;

import Entities.Contact;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContactDAO {
    private Connection conn;

    public ContactDAO() {
        this.conn = DatabaseConnection.getConnection();
    }


    public boolean addContact(int userId, int contactUserId, String nickname) {
        String sql = "INSERT INTO contacts (user_id, contact_user_id, nickname) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, contactUserId);
            pstmt.setString(3, nickname);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du contact: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteContact(int userId, int contactUserId) {
        String sql = "DELETE FROM contacts WHERE user_id = ? AND contact_user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, contactUserId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du contact: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }


    public int getUserIdByNickname(int userId, String nickname) {
        String sql = "SELECT contact_user_id FROM contacts WHERE user_id = ? AND nickname = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, nickname);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("contact_user_id") : -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }


    public boolean updateNickname(int userId, int contactUserId, String newNickname) {
        String sql = "UPDATE contacts SET nickname = ? WHERE user_id = ? AND contact_user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newNickname);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, contactUserId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du surnom: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<Contact> getContacts(int userId) {
        List<Contact> contacts = new ArrayList<>();
        String sql = "SELECT * FROM contacts WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                contacts.add(new Contact(
                        rs.getInt("contact_id"),
                        rs.getInt("user_id"),
                        rs.getInt("contact_user_id"),
                        rs.getString("nickname")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des contacts: " + e.getMessage());
            e.printStackTrace();
        }
        return contacts;
    }

    public boolean areContacts(int userId, int contactUserId) {
        String sql = "SELECT COUNT(*) FROM contacts WHERE (user_id = ? AND contact_user_id = ?) OR (user_id = ? AND contact_user_id = ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, contactUserId);
            pstmt.setInt(3, contactUserId);
            pstmt.setInt(4, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification du contact: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}