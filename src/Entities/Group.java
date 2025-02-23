package Entities;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a group in the chat application.
 */
public class Group {
    private int id;
    private String name;
    private String description;
    private int adminId;
    private Map<Integer, Boolean> membersMap; // Key: member ID, Value: connection status (true = connected, false = disconnected)

    /**
     * Constructs a new Group.
     *
     * @param id          The ID of the group.
     * @param name        The name of the group.
     * @param description The description of the group.
     * @param adminId     The ID of the group admin.
     */
    public Group(int id, String name, String description, int adminId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.adminId = adminId;
        this.membersMap = new HashMap<>();
    }

    // Getters
    public int getId() { return this.id; }
    public String getName() { return this.name; }
    public String getDescription() { return this.description; }
    public int getAdminId() { return this.adminId; }

    /**
     * Checks if a user is a member of the group.
     *
     * @param memberId The ID of the user.
     * @return True if the user is a member, false otherwise.
     */
    public boolean isMember(int memberId) {
        return this.membersMap.containsKey(memberId);
    }

    /**
     * Returns a string of disconnected member IDs, separated by newlines.
     *
     * @return A string of disconnected member IDs, or an empty string if none are disconnected.
     */
    public String getDisconnectedMembers() {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<Integer, Boolean> entry : membersMap.entrySet()) {
            if (!entry.getValue()) {
                result.append(entry.getKey()).append("\n");
            }
        }
        // Remove the last newline if there are any disconnected members
        if (result.length() > 0) {
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
    }

    /**
     * Marks a member as connected.
     *
     * @param memberId The ID of the member.
     */
    public void memberConnected(int memberId) {
        if (membersMap.containsKey(memberId)) {
            this.membersMap.put(memberId, true);
        }
    }

    /**
     * Marks a member as disconnected.
     *
     * @param memberId The ID of the member.
     */
    public void memberDisconnected(int memberId) {
        if (membersMap.containsKey(memberId)) {
            this.membersMap.put(memberId, false);
        }
    }

    /**
     * Checks if a member is connected.
     *
     * @param memberId The ID of the member.
     * @return True if the member is connected, false otherwise.
     */
    public boolean isConnected(int memberId) {
        return membersMap.containsKey(memberId) && membersMap.get(memberId);
    }

    /**
     * Adds a new member to the group.
     *
     * @param memberId The ID of the member.
     */
    public void addMember(int memberId) {
        this.membersMap.put(memberId, false); // New members are initially disconnected
    }

    /**
     * Removes a member from the group.
     *
     * @param memberId The ID of the member.
     */
    public void removeMember(int memberId) {
        this.membersMap.remove(memberId);
    }

    /**
     * Checks if all members are offline.
     *
     * @return True if all members are offline, false otherwise.
     */
    public boolean allOffline() {
        for (Boolean status : membersMap.values()) {
            if (status) {
                return false; // At least one member is online
            }
        }
        return true; // All members are offline
    }
}