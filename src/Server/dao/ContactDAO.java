package Server.dao;

import Server.entities.Contact;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContactDAO {
    private Connection conn;

    public ContactDAO(Connection conn) {
        this.conn = conn;
    }

    public boolean addContact(int userId, int contactUserId, String nickname) {
        // Vérifier si la relation spécifique existe déjà avant d'insérer (sécurité BDD)
        if (doesSpecificContactExist(userId, contactUserId)) {
            System.err.println("[DAO WARN] Tentative d'ajout de contact déjà existant: " + userId + " -> " + contactUserId);
            return false; // Ou gérer autrement, mais l'unicité devrait être gérée par la logique appelante
        }
        String sql = "INSERT INTO contacts (user_id, contact_user_id, nickname) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, contactUserId);
            pstmt.setString(3, nickname);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            // Gérer la violation de contrainte unique si elle existe sur (user_id, contact_user_id)
            if (e.getErrorCode() == 1062) { // Code MySQL pour entrée dupliquée
                System.err.println("[DAO WARN] Violation contrainte unique lors de l'ajout contact: " + userId + " -> " + contactUserId);
                return false;
            }
            System.err.println("Erreur lors de l'ajout du contact: " + e.getMessage());
            e.printStackTrace(); // Garder pour le debug
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

    /**
     * Trouve l'ID du contact basé sur le surnom donné par un utilisateur spécifique.
     */
    public int getUserIdByNickname(int userId, String nickname) {
        String sql = "SELECT contact_user_id FROM contacts WHERE user_id = ? AND nickname = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, nickname);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("contact_user_id") : -1; // Retourne ID ou -1 si non trouvé
        } catch (SQLException e) {
            System.err.println("Erreur recherche contact par surnom: " + e.getMessage());
            // e.printStackTrace(); // Optionnel
            return -1;
        }
    }


    public boolean updateNickname(int userId, int contactUserId, String newNickname) {
        // Ajouter une vérification si le nouveau surnom est déjà utilisé par cet utilisateur pour un AUTRE contact
        int existingContactWithNewNickname = getUserIdByNickname(userId, newNickname);
        if (existingContactWithNewNickname != -1 && existingContactWithNewNickname != contactUserId) {
            System.err.println("[DAO WARN] Update nickname failed: New nickname '" + newNickname + "' already used by user " + userId + " for contact " + existingContactWithNewNickname);
            return false; // Empêcher la mise à jour si le nouveau surnom est déjà pris
        }

        String sql = "UPDATE contacts SET nickname = ? WHERE user_id = ? AND contact_user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newNickname);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, contactUserId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // Gérer la violation de contrainte unique si elle existe sur (user_id, nickname)
            if (e.getErrorCode() == 1062) {
                System.err.println("[DAO WARN] Violation contrainte unique lors MAJ surnom pour user " + userId);
                return false;
            }
            System.err.println("Erreur lors de la mise à jour du surnom: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Récupère la liste des contacts ajoutés PAR l'utilisateur spécifié.
     */
    public List<Contact> getContacts(int userId) {
        List<Contact> contacts = new ArrayList<>();
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
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des contacts: " + e.getMessage());
            e.printStackTrace();
        }
        return contacts;
    }

    /**
     * Vérifie s'il existe une relation de contact DANS UN SENS OU L'AUTRE.
     * Utile pour savoir si deux personnes sont "connectées" d'une manière ou d'une autre.
     */
    public boolean areContacts(int userId1, int userId2) {
        String sql = "SELECT COUNT(*) FROM contacts WHERE (user_id = ? AND contact_user_id = ?) OR (user_id = ? AND contact_user_id = ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId1);
            pstmt.setInt(2, userId2);
            pstmt.setInt(3, userId2); // Inverse
            pstmt.setInt(4, userId1); // Inverse
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification (bidirectionnelle) du contact: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ***** NOUVELLE MÉTHODE *****
    /**
     * Vérifie si un utilisateur spécifique a déjà ajouté un autre utilisateur spécifique comme contact.
     * @param userId L'ID de l'utilisateur qui ajoute (ou aurait ajouté).
     * @param contactUserId L'ID de l'utilisateur ajouté (ou qui aurait été ajouté).
     * @return true si la relation userId -> contactUserId existe, false sinon.
     */
    public boolean doesSpecificContactExist(int userId, int contactUserId) {
        String sql = "SELECT 1 FROM contacts WHERE user_id = ? AND contact_user_id = ? LIMIT 1"; // Optimisé: 1 et LIMIT 1
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, contactUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Retourne true si au moins une ligne est trouvée
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification du contact spécifique (" + userId + " -> " + contactUserId + "): " + e.getMessage());
            // e.printStackTrace(); // Optionnel pour debug
        }
        return false; // Retourne false en cas d'erreur ou si non trouvé
    }
    // ***** FIN NOUVELLE MÉTHODE *****
}