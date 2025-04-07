package Server.controllers;

import Server.entities.Group;
import Server.entities.User;
import Server.dao.GroupDAO;
import Server.dao.MessageDAO;
import Server.dao.UserDAO;
import Server.utils.AnsiColors;
import Server.utils.ValidationUtils;
import Server.views.HelpView;
import Server.views.MenuView;

import java.io.*;
import java.net.SocketException;
// Imports pour la logique de fichier si elle reste ici
import java.time.format.DateTimeFormatter;
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

    // Constantes pour ce contrôleur
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String SERVER_STORAGE = "src/uploads/"; // Garder si handleGroupFileUpload reste ici

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
                menuView.showGroupsMenu(); // Affiche 1-8
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
                    case "7": handleGroupChatSession(); break; // Lance le chat (ne fait pas de prompt ici)
                    case "8": backToUserMenu = true; break;
                    default: menuView.sendFeedback("Invalid choice (1-8).", AnsiColors.RED); menuView.promptContinue(); break;
                }
            } catch (EOFException | SocketException e) { throw e; } // Remonter
            catch (IOException e) { menuView.sendFeedback("IO Error: " + e.getMessage(), AnsiColors.RED); System.err.println("GroupController IO Err: "+e); menuView.promptContinue(); }
            catch (Exception e) { menuView.sendFeedback("Error: " + e.getMessage(), AnsiColors.RED); System.err.println("GroupController Err: "+e); e.printStackTrace(); menuView.promptContinue(); }
        }
    }

    private void createGroup() throws IOException {
        dos.writeUTF("\nNom du nouveau groupe:"); dos.flush();
        String name = dis.readUTF();
        if (name == null || name.trim().isEmpty()) { menuView.sendFeedback("Nom vide.", AnsiColors.RED); return; }
        name = name.trim();
        if(groupDAO.getGroupIdByName(name) != -1) { menuView.sendFeedback("Nom '" + name + "' déjà pris.", AnsiColors.RED); return; }
        dos.writeUTF("Description pour '" + name + "':"); dos.flush();
        String description = dis.readUTF(); description = (description == null) ? "" : description.trim();
        boolean success = groupDAO.createGroup(name, description, userAccount.getId());
        if (success) menuView.sendFeedback("Groupe '" + name + "' créé.", AnsiColors.GREEN);
        else menuView.sendFeedback("Échec création.", AnsiColors.RED);
    }

    private void joinGroup() throws IOException {
        dos.writeUTF("\nNom du groupe à rejoindre:"); dos.flush();
        String groupName = dis.readUTF();
        if (groupName == null || groupName.trim().isEmpty()) { menuView.sendFeedback("Nom invalide.", AnsiColors.RED); return; }
        groupName = groupName.trim();
        int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) { menuView.sendFeedback("Groupe '" + groupName + "' non trouvé.", AnsiColors.RED); return; }
        if (groupDAO.getGroupMembers(groupId).contains(userAccount.getId())) { menuView.sendFeedback("Déjà membre.", AnsiColors.YELLOW); return; }
        boolean success = groupDAO.addUserToGroup(userAccount.getId(), groupId);
        if(success) menuView.sendFeedback("Rejoint '" + groupName + "'.", AnsiColors.GREEN);
        else menuView.sendFeedback("Échec pour rejoindre.", AnsiColors.RED);
    }

    private void addMember() throws IOException {
        dos.writeUTF("\nNom du groupe où ajouter:"); dos.flush();
        String groupName = dis.readUTF();
        if (groupName == null || groupName.trim().isEmpty()) { menuView.sendFeedback("Nom invalide.", AnsiColors.RED); return; }
        groupName = groupName.trim();
        int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) { menuView.sendFeedback("Groupe '" + groupName + "' non trouvé.", AnsiColors.RED); return; }
        if (!groupDAO.isGroupAdmin(userAccount.getId(), groupId)) { menuView.sendFeedback("Seul l'admin peut ajouter.", AnsiColors.RED); /*SoundPlayer...*/ return; }
        dos.writeUTF("Email de l'utilisateur à ajouter:"); dos.flush();
        String emailToAdd = dis.readUTF();
        if (emailToAdd == null || !ValidationUtils.isValidEmail(emailToAdd.trim())) { menuView.sendFeedback("Email invalide.", AnsiColors.RED); return; }
        emailToAdd = emailToAdd.trim().toLowerCase();
        int userIdToAdd = userDAO.getUserIdByEmail(emailToAdd);
        if (userIdToAdd == -1) { menuView.sendFeedback("Utilisateur '" + emailToAdd + "' non trouvé.", AnsiColors.RED); return; }
        if (groupDAO.getGroupMembers(groupId).contains(userIdToAdd)) { menuView.sendFeedback("'" + emailToAdd + "' déjà membre.", AnsiColors.YELLOW); return; }
        boolean success = groupDAO.addUserToGroup(userIdToAdd, groupId);
        if(success) { menuView.sendFeedback("'" + emailToAdd + "' ajouté.", AnsiColors.GREEN); notifyUserAddedToGroup(userIdToAdd, groupName); }
        else { menuView.sendFeedback("Échec ajout.", AnsiColors.RED); }
    }

    private void removeMember() throws IOException {
        dos.writeUTF("\nNom du groupe où supprimer:"); dos.flush();
        String groupName = dis.readUTF();
        if (groupName == null || groupName.trim().isEmpty()) { menuView.sendFeedback("Nom invalide.", AnsiColors.RED); return; }
        groupName = groupName.trim();
        int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) { menuView.sendFeedback("Groupe '" + groupName + "' non trouvé.", AnsiColors.RED); return; }
        if (!groupDAO.isGroupAdmin(userAccount.getId(), groupId)) { menuView.sendFeedback("Seul l'admin peut supprimer.", AnsiColors.RED); /*SoundPlayer...*/ return; }
        dos.writeUTF("Email de l'utilisateur à supprimer:"); dos.flush();
        String emailToRemove = dis.readUTF();
        if (emailToRemove == null || !ValidationUtils.isValidEmail(emailToRemove.trim())) { menuView.sendFeedback("Email invalide.", AnsiColors.RED); return; }
        emailToRemove = emailToRemove.trim().toLowerCase();
        int userIdToRemove = userDAO.getUserIdByEmail(emailToRemove);
        if (userIdToRemove == -1) { menuView.sendFeedback("Utilisateur '" + emailToRemove + "' non trouvé.", AnsiColors.RED); return; }
        if (userIdToRemove == userAccount.getId()) { menuView.sendFeedback("L'admin ne peut se supprimer.", AnsiColors.YELLOW); return; }
        if (!groupDAO.getGroupMembers(groupId).contains(userIdToRemove)) { menuView.sendFeedback("'" + emailToRemove + "' n'est pas membre.", AnsiColors.YELLOW); return; }
        boolean success = groupDAO.removeUserFromGroup(userIdToRemove, groupId);
        if (success) { menuView.sendFeedback("'" + emailToRemove + "' supprimé.", AnsiColors.GREEN); notifyUserRemovedFromGroup(userIdToRemove, groupName); }
        else { menuView.sendFeedback("Échec suppression.", AnsiColors.RED); }
    }

    private void listUserGroups() throws IOException {
        List<Group> groups = groupDAO.getGroupsForUser(userAccount.getId());
        if (groups.isEmpty()) { dos.writeUTF("\nAucun groupe.\r\n"); dos.flush(); return; }
        StringBuilder sb = new StringBuilder("\r\n--- Vos Groupes ---\r\n");
        for (Group g : groups) {
            boolean isAdmin = groupDAO.isGroupAdmin(userAccount.getId(), g.getId());
            String adminTag = isAdmin ? AnsiColors.YELLOW+" (Admin)"+AnsiColors.RESET : "";
            sb.append(String.format(" %s (%s)%s%n", AnsiColors.MAGENTA+g.getName()+AnsiColors.RESET, g.getDescription(), adminTag));
        }
        sb.append("-----------------\r\n"); dos.writeUTF(sb.toString()); dos.flush();
    }

    private void listGroupMembers() throws IOException {
        dos.writeUTF("\nNom du groupe:"); dos.flush(); String groupName = dis.readUTF();
        if (groupName == null || groupName.trim().isEmpty()) { menuView.sendFeedback("Nom invalide.", AnsiColors.RED); return; }
        groupName = groupName.trim(); int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) { menuView.sendFeedback("Groupe '" + groupName + "' non trouvé.", AnsiColors.RED); return; }
        List<Integer> memberIds = groupDAO.getGroupMembers(groupId);
        if (!memberIds.contains(userAccount.getId())) { menuView.sendFeedback("Vous n'êtes pas membre.", AnsiColors.RED); return; }
        dos.writeUTF("\n--- Membres de '" + groupName + "' ---");
        if (memberIds.isEmpty()) { dos.writeUTF(" (Aucun membre)"); }
        else { for (int mId : memberIds) { String email = userDAO.getEmailById(mId); boolean isAdmin = groupDAO.isGroupAdmin(mId, groupId); String aTag = isAdmin ? AnsiColors.YELLOW+" (Admin)"+AnsiColors.RESET : ""; if (email != null) { String status = onlineUserStreams.containsKey(email.toLowerCase()) ? AnsiColors.GREEN+"[En ligne]" : AnsiColors.RED+"[Hors ligne]"; dos.writeUTF("- "+AnsiColors.CYAN+email+AnsiColors.RESET+" "+status+AnsiColors.RESET+aTag); } else { dos.writeUTF("- ID " + mId + AnsiColors.GRAY+" [Email inconnu]"+AnsiColors.RESET+aTag); } } }
        dos.writeUTF("---------------------------"); dos.flush();
    }

    // Lance la session de chat via ChatController
    private void handleGroupChatSession() throws IOException {
        if (chatController != null) {
            chatController.handleGroupChatSession();
        } else {
            System.err.println("[ERROR] ChatController is not set in GroupController!");
            menuView.sendFeedback("Erreur interne: Impossible de démarrer le chat.", AnsiColors.RED);
        }
    }

    // --- Méthodes de notification (pourraient être dans un service) ---
    private void notifyUserAddedToGroup(int userId, String groupName) {
        String email = userDAO.getEmailById(userId);
        if (email != null && onlineUserStreams.containsKey(email.toLowerCase())) {
            try { onlineUserStreams.get(email.toLowerCase()).writeUTF("\n"+AnsiColors.GREEN+"[INFO] Ajouté au groupe '"+groupName+"'.\n> "+AnsiColors.RESET); onlineUserStreams.get(email.toLowerCase()).flush(); }
            catch (IOException e) { /* ignore */ }
        }
    }

    private void notifyUserRemovedFromGroup(int userId, String groupName) {
        String email = userDAO.getEmailById(userId);
        if (email != null && onlineUserStreams.containsKey(email.toLowerCase())) {
            try { onlineUserStreams.get(email.toLowerCase()).writeUTF("\n"+AnsiColors.RED+"[INFO] Retiré du groupe '"+groupName+"'.\n> "+AnsiColors.RESET); onlineUserStreams.get(email.toLowerCase()).flush(); }
            catch (IOException e) { /* ignore */ }
        }
    }
}