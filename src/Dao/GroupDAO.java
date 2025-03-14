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
        String query = "SELECT g.Groupe_id, g.Groupe_name, g.Groupe_description, g.Groupe_admin_id FROM groupe g " +
                "JOIN users_groups ug ON g.Groupe_id = ug.group_id " +
                "WHERE ug.user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Group group = new Group(rs.getInt("Groupe_id"), rs.getString("Groupe_name"), rs.getString("Groupe_description"), rs.getInt("Groupe_admin_id"));
                groups.add(group);
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
    public boolean removeUserFromGroup(int userId, int groupId) {
        String query = "DELETE FROM users_groups WHERE user_id = ? AND group_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, groupId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isGroupAdmin(int userId, int groupId) {
        String sql = "SELECT * FROM groupe WHERE Groupe_id = ? AND Groupe_admin_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
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


    public int getGroupIdByName(String groupName) {
        String sql = "SELECT Groupe_id FROM groupe WHERE groupe_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("Groupe_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    public List<Integer> getGroupMembers(int groupId) {
        List<Integer> memberIds = new ArrayList<>();
        String query = "SELECT user_id FROM users_groups WHERE group_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                memberIds.add(rs.getInt("user_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return memberIds;
    }

}