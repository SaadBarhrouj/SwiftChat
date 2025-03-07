package Dao;

import Entities.Contact;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe ContactDAO permettant la gestion des contacts dans la base de données.
 * Elle fournit des méthodes pour ajouter, supprimer, mettre à jour et récupérer des contacts.
 */
public class ContactDAO {
    private Connection conn;

    /**
     * Constructeur qui initialise la connexion à la base de données.
     */
    public ContactDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    /**
     * Ajoute un contact pour un utilisateur donné.
     *
     * @param userId        ID de l'utilisateur ajoutant le contact.
     * @param contactUserId ID de l'utilisateur ajouté en contact.
     * @param nickname      Surnom associé au contact.
     * @return true si l'ajout est réussi, false sinon.
     */
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

    /**
     * Supprime un contact d'un utilisateur donné.
     *
     * @param userId        ID de l'utilisateur.
     * @param contactUserId ID du contact à supprimer.
     * @return true si la suppression est réussie, false sinon.
     */
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

    /**
     * Met à jour le surnom d'un contact d'un utilisateur.
     *
     * @param userId        ID de l'utilisateur.
     * @param contactUserId ID du contact dont le surnom doit être modifié.
     * @param newNickname   Nouveau surnom du contact.
     * @return true si la mise à jour est réussie, false sinon.
     */
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

    /**
     * Récupère la liste des contacts d'un utilisateur donné.
     *
     * @param userId ID de l'utilisateur.
     * @return Liste des contacts de l'utilisateur.
     */
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

    /**
     * Vérifie si deux utilisateurs sont en contact.
     *
     * @param userId        ID du premier utilisateur.
     * @param contactUserId ID du second utilisateur.
     * @return true si les utilisateurs sont en contact, false sinon.
     */
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