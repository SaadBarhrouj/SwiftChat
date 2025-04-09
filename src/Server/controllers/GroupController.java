package Server.controllers;

import Server.entities.*;
import Server.dao.*;
import Server.utils.*;
import Server.views.*;
import java.io.*;
import java.net.SocketException;
import java.util.List;
import java.util.Map;

public class GroupController {

    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final GroupDAO groupDAO;
    private final UserDAO userDAO;
    private final MessageDAO messageDAO;
    private final MenuView menuView;
    private final HelpView helpView;
    private final User userAccount;
    private final Map<String, DataOutputStream> onlineUserStreams;

    private ChatController chatController;

    public GroupController(DataInputStream dis, DataOutputStream dos, GroupDAO groupDAO, UserDAO userDAO, MessageDAO messageDAO, MenuView menuView, HelpView helpView, User userAccount, Map<String, DataOutputStream> onlineUserStreams) {
        this.dis = dis;
        this.dos = dos;
        this.groupDAO = groupDAO;
        this.userDAO = userDAO;
        this.messageDAO = messageDAO;
        this.menuView = menuView;
        this.helpView = helpView;
        this.userAccount = userAccount;
        this.onlineUserStreams = onlineUserStreams;
    }

    public void setChatController(ChatController chatController) {
        this.chatController = chatController;
    }

    public void handleMenu() throws IOException {
        boolean backToUserMenu = false;
        while (!backToUserMenu) {
            try {
                menuView.showGroupsMenu(); // Mettez à jour cette méthode pour afficher l'option 9
                String choice = dis.readUTF();
                if (choice == null) throw new EOFException("Client disconnected during groups menu.");
                choice = choice.trim();

                switch (choice) {
                    case "1": createGroup(); menuView.promptContinue(); break;
                    case "2": joinGroup(); menuView.promptContinue(); break;
                    case "3": addMember(); menuView.promptContinue(); break;
                    case "4": removeMember(); menuView.promptContinue(); break;
                    case "5": listUserGroups(); menuView.promptContinue(); break;
                    case "6": listGroupMembers(); menuView.promptContinue(); break;
                    case "7": handleGroupChatSession(); break;
                    case "8": leaveGroup(); menuView.promptContinue(); break; // Nouvelle option
                    case "9": backToUserMenu = true; break; // Déplacé à 9
                    default: menuView.sendFeedback("Invalid choice (1-9).", AnsiColors.RED); menuView.promptContinue(); break;
                }
            } catch (EOFException | SocketException e) { throw e; }
            catch (IOException e) { menuView.sendFeedback("IO Error: " + e.getMessage(), AnsiColors.RED); System.err.println("GroupController IO Err: "+e); menuView.promptContinue(); }
            catch (Exception e) { menuView.sendFeedback("Error: " + e.getMessage(), AnsiColors.RED); System.err.println("GroupController Err: "+e); e.printStackTrace(); menuView.promptContinue(); }
        }
    }

    private void leaveGroup() throws IOException {
        dos.writeUTF("\nName of group to leave:"); dos.flush();
        String groupName = dis.readUTF();
        if (groupName == null || groupName.trim().isEmpty()) {
            menuView.sendFeedback("Invalid name.", AnsiColors.RED);
            return;
        }
        groupName = groupName.trim();

        int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) {
            menuView.sendFeedback("Group '" + groupName + "' not found.", AnsiColors.RED);
            return;
        }


        if (groupDAO.isGroupAdmin(userAccount.getId(), groupId)) {
            menuView.sendFeedback("Admins cannot leave the group. Please transfer admin rights first or delete the group.", AnsiColors.RED);
            return;
        }


        if (!groupDAO.getGroupMembers(groupId).contains(userAccount.getId())) {
            menuView.sendFeedback("You're not a member of this group.", AnsiColors.YELLOW);
            return;
        }

        boolean success = groupDAO.removeUserFromGroup(userAccount.getId(), groupId);
        if (success) {
            menuView.sendFeedback("You have left the group '" + groupName + "' successfully.", AnsiColors.GREEN);
        } else {
            menuView.sendFeedback("Failed to leave the group.", AnsiColors.RED);
        }
    }

    private void createGroup() throws IOException {
        dos.writeUTF("\nNew group name:"); dos.flush();
        String name = dis.readUTF();
        if (name == null || name.trim().isEmpty()) { menuView.sendFeedback("Name cannot be empty.", AnsiColors.RED); return; }
        name = name.trim();
        if(groupDAO.getGroupIdByName(name) != -1) { menuView.sendFeedback("Name '" + name + "' is already taken.", AnsiColors.RED); return; }
        dos.writeUTF("Description for '" + name + "':"); dos.flush();
        String description = dis.readUTF(); description = (description == null) ? "" : description.trim();
        boolean success = groupDAO.createGroup(name, description, userAccount.getId());
        if (success) menuView.sendFeedback("Group '" + name + "' created successfully.", AnsiColors.GREEN);
        else menuView.sendFeedback("Error creating group.", AnsiColors.RED);
    }

    private void joinGroup() throws IOException {
        dos.writeUTF("\nName of group to join:"); dos.flush();
        String groupName = dis.readUTF();
        if (groupName == null || groupName.trim().isEmpty()) { menuView.sendFeedback("Invalid name.", AnsiColors.RED); return; }
        groupName = groupName.trim();
        int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) { menuView.sendFeedback("Group '" + groupName + "' not found.", AnsiColors.RED); return; }
        if (groupDAO.getGroupMembers(groupId).contains(userAccount.getId())) { menuView.sendFeedback("You're already a member.", AnsiColors.YELLOW); return; }
        boolean success = groupDAO.addUserToGroup(userAccount.getId(), groupId);
        if(success) menuView.sendFeedback("Joined '" + groupName + "' successfully.", AnsiColors.GREEN);
        else menuView.sendFeedback("Error joining group.", AnsiColors.RED);
    }

    private void addMember() throws IOException {
        dos.writeUTF("\nGroup name to add to:"); dos.flush();
        String groupName = dis.readUTF();
        if (groupName == null || groupName.trim().isEmpty()) { menuView.sendFeedback("Invalid name.", AnsiColors.RED); return; }
        groupName = groupName.trim();
        int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) { menuView.sendFeedback("Group '" + groupName + "' not found.", AnsiColors.RED); return; }
        if (!groupDAO.isGroupAdmin(userAccount.getId(), groupId)) { menuView.sendFeedback("Only admin can add members.", AnsiColors.RED); return; }
        dos.writeUTF("Email of user to add:"); dos.flush();
        String emailToAdd = dis.readUTF();
        if (emailToAdd == null || !ValidationUtils.isValidEmail(emailToAdd.trim())) { menuView.sendFeedback("Invalid email.", AnsiColors.RED); return; }
        emailToAdd = emailToAdd.trim().toLowerCase();
        int userIdToAdd = userDAO.getUserIdByEmail(emailToAdd);
        if (userIdToAdd == -1) { menuView.sendFeedback("User '" + emailToAdd + "' not found.", AnsiColors.RED); return; }
        if (groupDAO.getGroupMembers(groupId).contains(userIdToAdd)) { menuView.sendFeedback("'" + emailToAdd + "' is already a member.", AnsiColors.YELLOW); return; }
        boolean success = groupDAO.addUserToGroup(userIdToAdd, groupId);
        if(success) { menuView.sendFeedback("'" + emailToAdd + "' added successfully.", AnsiColors.GREEN); notifyUserAddedToGroup(userIdToAdd, groupName); }
        else { menuView.sendFeedback("Failed to add member.", AnsiColors.RED); }
    }

    private void removeMember() throws IOException {
        dos.writeUTF("\nGroup name to remove from:"); dos.flush();
        String groupName = dis.readUTF();
        if (groupName == null || groupName.trim().isEmpty()) { menuView.sendFeedback("Invalid name.", AnsiColors.RED); return; }
        groupName = groupName.trim();
        int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) { menuView.sendFeedback("Group '" + groupName + "' not found.", AnsiColors.RED); return; }
        if (!groupDAO.isGroupAdmin(userAccount.getId(), groupId)) { menuView.sendFeedback("Only admin can remove members.", AnsiColors.RED); return; }
        dos.writeUTF("Email of user to remove:"); dos.flush();
        String emailToRemove = dis.readUTF();
        if (emailToRemove == null || !ValidationUtils.isValidEmail(emailToRemove.trim())) { menuView.sendFeedback("Invalid email.", AnsiColors.RED); return; }
        emailToRemove = emailToRemove.trim().toLowerCase();
        int userIdToRemove = userDAO.getUserIdByEmail(emailToRemove);
        if (userIdToRemove == -1) { menuView.sendFeedback("User '" + emailToRemove + "' not found.", AnsiColors.RED); return; }
        if (userIdToRemove == userAccount.getId()) { menuView.sendFeedback("Admin cannot remove themselves.", AnsiColors.YELLOW); return; }
        if (!groupDAO.getGroupMembers(groupId).contains(userIdToRemove)) { menuView.sendFeedback("'" + emailToRemove + "' is not a member.", AnsiColors.YELLOW); return; }
        boolean success = groupDAO.removeUserFromGroup(userIdToRemove, groupId);
        if (success) { menuView.sendFeedback("'" + emailToRemove + "' removed successfully.", AnsiColors.GREEN); notifyUserRemovedFromGroup(userIdToRemove, groupName); }
        else { menuView.sendFeedback("Failed to remove member.", AnsiColors.RED); }
    }

    private void listUserGroups() throws IOException {
        List<Group> groups = groupDAO.getGroupsForUser(userAccount.getId());
        if (groups.isEmpty()) { dos.writeUTF("\nNo groups.\r\n"); dos.flush(); return; }
        StringBuilder sb = new StringBuilder("\r\n--- Your Groups ---\r\n");
        for (Group g : groups) {
            boolean isAdmin = groupDAO.isGroupAdmin(userAccount.getId(), g.getId());
            String adminTag = isAdmin ? AnsiColors.YELLOW+" (Admin)"+AnsiColors.RESET : "";
            sb.append(String.format(" %s (%s)%s%n", AnsiColors.MAGENTA+g.getName()+AnsiColors.RESET, g.getDescription(), adminTag));
        }
        sb.append("-----------------\r\n"); dos.writeUTF(sb.toString()); dos.flush();
    }

    private void listGroupMembers() throws IOException {
        dos.writeUTF("\nGroup name:"); dos.flush(); String groupName = dis.readUTF();
        if (groupName == null || groupName.trim().isEmpty()) { menuView.sendFeedback("Invalid name.", AnsiColors.RED); return; }
        groupName = groupName.trim(); int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) { menuView.sendFeedback("Group '" + groupName + "' not found.", AnsiColors.RED); return; }
        List<Integer> memberIds = groupDAO.getGroupMembers(groupId);
        if (!memberIds.contains(userAccount.getId())) { menuView.sendFeedback("You're not a member.", AnsiColors.RED); return; }
        dos.writeUTF("\n--- Members of '" + groupName + "' ---");
        if (memberIds.isEmpty()) { dos.writeUTF(" (No members)"); }
        else {
            for (int mId : memberIds) {
                String email = userDAO.getEmailById(mId);
                boolean isAdmin = groupDAO.isGroupAdmin(mId, groupId);
                String aTag = isAdmin ? AnsiColors.YELLOW+" (Admin)"+AnsiColors.RESET : "";
                if (email != null) {
                    String status = onlineUserStreams.containsKey(email.toLowerCase()) ? AnsiColors.GREEN+"[Online]" : AnsiColors.RED+"[Offline]";
                    dos.writeUTF("- "+AnsiColors.CYAN+email+AnsiColors.RESET+" "+status+AnsiColors.RESET+aTag);
                } else {
                    dos.writeUTF("- ID " + mId + AnsiColors.GRAY+" [Unknown email]"+AnsiColors.RESET+aTag);
                }
            }
        }
        dos.writeUTF("---------------------------"); dos.flush();
    }


    private void handleGroupChatSession() throws IOException {
        if (chatController != null) {
            chatController.handleGroupChatSession();
        } else {
            System.err.println("[ERROR] ChatController is not set in GroupController!");
            menuView.sendFeedback("Internal error: Cannot start chat.", AnsiColors.RED);
        }
    }


    private void notifyUserAddedToGroup(int userId, String groupName) {
        String email = userDAO.getEmailById(userId);
        if (email != null && onlineUserStreams.containsKey(email.toLowerCase())) {
            try {
                onlineUserStreams.get(email.toLowerCase()).writeUTF("\n"+AnsiColors.GREEN+"[INFO] Added to group '"+groupName+"'.\n> "+AnsiColors.RESET);
                onlineUserStreams.get(email.toLowerCase()).flush();
            } catch (IOException e) { }
        }
    }

    private void notifyUserRemovedFromGroup(int userId, String groupName) {
        String email = userDAO.getEmailById(userId);
        if (email != null && onlineUserStreams.containsKey(email.toLowerCase())) {
            try {
                onlineUserStreams.get(email.toLowerCase()).writeUTF("\n"+AnsiColors.RED+"[INFO] Removed from group '"+groupName+"'.\n> "+AnsiColors.RESET);
                onlineUserStreams.get(email.toLowerCase()).flush();
            } catch (IOException e) {

            }
        }
    }
}