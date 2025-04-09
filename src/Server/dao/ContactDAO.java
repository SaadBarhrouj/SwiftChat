package Server.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import Server.entities.Contact;
import Server.persistence.SerializationManager;
import Server.utils.AppPaths;

public class ContactDAO {
    private Connection conn;

    public ContactDAO(Connection conn) {
        this.conn = conn;
        new File(AppPaths.SERVER_CONTACTS_SERIALIZATION_DIR).mkdirs();
    }

    public boolean addContact(int userId, int contactUserId, String nickname) {

        if (doesSpecificContactExist(userId, contactUserId)) {
            System.err.println("[DAO WARN] Attempt to add existing contact: " + userId + " -> " + contactUserId);
            return false;
        }
        String sql = "INSERT INTO contacts (user_id, contact_user_id, nickname) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, contactUserId);
            pstmt.setString(3, nickname);
            int rowsAffected = pstmt.executeUpdate();
            backupUserContacts(userId);
            return rowsAffected > 0;
        } catch (SQLException e) {

            if (e.getErrorCode() == 1062) {
                System.err.println("[DAO WARN] Unique constraint violation when adding contact: " + userId + " -> " + contactUserId);
                return false;
            }
            System.err.println("Error adding contact: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteContact(int userId, int contactUserId) {
        String sql = "DELETE FROM contacts WHERE user_id = ? AND contact_user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, contactUserId);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                backupUserContacts(userId);
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error deleting contact: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public int getUserIdByNickname(int userId, String nickname) {
        List<Contact> contacts = loadUserContacts(userId);
        for (Contact c : contacts) {
            if (c.getNickname().equalsIgnoreCase(nickname)) {
                return c.getContactUserId();
            }
        }
        String sql = "SELECT contact_user_id FROM contacts WHERE user_id = ? AND nickname = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, nickname);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("contact_user_id") : -1;
        } catch (SQLException e) {
            System.err.println("Error searching contact by nickname: " + e.getMessage());

            return -1;
        }
    }

    public boolean updateNickname(int userId, int contactUserId, String newNickname) {

        int existingContactWithNewNickname = getUserIdByNickname(userId, newNickname);
        if (existingContactWithNewNickname != -1 && existingContactWithNewNickname != contactUserId) {
            System.err.println("[DAO WARN] Update nickname failed: New nickname '" + newNickname + "' already used by user " + userId + " for contact " + existingContactWithNewNickname);
            return false;
        }

        String sql = "UPDATE contacts SET nickname = ? WHERE user_id = ? AND contact_user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newNickname);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, contactUserId);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                backupUserContacts(userId);
            }
            return success;
        } catch (SQLException e) {

            if (e.getErrorCode() == 1062) {
                System.err.println("[DAO WARN] Unique constraint violation when updating nickname for user " + userId);
                return false;
            }
            System.err.println("Error updating nickname: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }


    public List<Contact> getContacts(int userId) {
        List<Contact> contacts = loadUserContacts(userId);
        if (!contacts.isEmpty()) {
            return contacts;
        }
        contacts = new ArrayList<>();
        String sql = "SELECT contact_id, user_id, contact_user_id, nickname FROM contacts WHERE user_id = ?";
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
            backupUserContacts(userId, contacts);
        } catch (SQLException e) {
            System.err.println("Error retrieving contacts: " + e.getMessage());
            e.printStackTrace();
        }
        return contacts;
    }



    public boolean doesSpecificContactExist(int userId, int contactUserId) {
        String sql = "SELECT 1 FROM contacts WHERE user_id = ? AND contact_user_id = ? LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, contactUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking specific contact (" + userId + " -> " + contactUserId + "): " + e.getMessage());

        }
        return false;
    }

    private void backupUserContacts(int userId) {
        List<Contact> contacts = getContactsFromDB(userId);
        backupUserContacts(userId, contacts);
    }

    private void backupUserContacts(int userId, List<Contact> contacts) {
        String filePath = AppPaths.SERVER_CONTACTS_SERIALIZATION_DIR + "user_" + userId + "_contacts.ser";
        try {
            SerializationManager.serialize(contacts, filePath);
        } catch (Exception e) {
            System.err.println("Error saving contacts for user " + userId + ": " + e.getMessage());
        }
    }

    private List<Contact> loadUserContacts(int userId) {
        String filePath = AppPaths.SERVER_CONTACTS_SERIALIZATION_DIR + "user_" + userId + "_contacts.ser";
        try {
            return (List<Contact>) SerializationManager.deserialize(filePath);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<Contact> getContactsFromDB(int userId) {
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
            System.err.println("Error retrieving contacts from DB: " + e.getMessage());
        }
        return contacts;
    }
}