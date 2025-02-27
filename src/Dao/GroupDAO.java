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
        String sql = "INSERT INTO Groupe (groupe_name, groupe_description, Groupe_admin_id) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setInt(3, adminId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Group getGroupById(int groupId) {
        String sql = "SELECT * FROM Groupe WHERE group_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Group(rs.getInt("group_id"), rs.getString("groupe_name"), rs.getString("groupe_description"), rs.getInt("Groupe_admin_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}