package Dao;

import Entities.Group;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupDAO {
    private Connection conn;

    public GroupDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    public List<Group> getGroupsForUser(int userId) {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT g.* FROM Groupe g JOIN users_groups ug ON g.group_id = ug.group_id WHERE ug.user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                groups.add(new Group(rs.getInt("group_id"), rs.getString("groupe_name"), rs.getString("groupe_description"), rs.getInt("Groupe_admin_id")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return groups;
    }

    public boolean createGroup(String name, String description, int adminId) {
        String sqlGroup = "INSERT INTO Groupe (groupe_name, groupe_description, Groupe_admin_id) VALUES (?, ?, ?)";
        String sqlUserGroup = "INSERT INTO users_groups (user_id, group_id) VALUES (?, ?)";

        try (PreparedStatement pstmtGroup = conn.prepareStatement(sqlGroup, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmtUserGroup = conn.prepareStatement(sqlUserGroup)) {


            conn.setAutoCommit(false);


            pstmtGroup.setString(1, name);
            pstmtGroup.setString(2, description);
            pstmtGroup.setInt(3, adminId);
            int affectedRows = pstmtGroup.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("La création du groupe a échoué, aucune ligne affectée.");
            }


            int groupId;
            try (ResultSet generatedKeys = pstmtGroup.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    groupId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("La création du groupe a échoué, aucun ID obtenu.");
                }
            }

            pstmtUserGroup.setInt(1, adminId);
            pstmtUserGroup.setInt(2, groupId);
            pstmtUserGroup.executeUpdate();


            conn.commit();
            return true;

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Group getGroupById(int groupId) {
        String sql = "SELECT * FROM groupe WHERE Groupe_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Group(
                        rs.getInt("Groupe_id"),
                        rs.getString("Groupe_name"),
                        rs.getString("Groupe_description"),
                        rs.getInt("Groupe_admin_id")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addUserToGroup(int userId, int groupId) {
        String sql = "INSERT INTO users_groups (user_id, group_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, groupId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}