package Server.controllers;

import Server.dao.*;
import Server.entities.*;
import Server.utils.*;
import Server.views.*;

import java.io.*;
import java.net.SocketException;
import java.util.List;
import java.util.Map;

public class ContactController {

    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final ContactDAO contactDAO;
    private final UserDAO userDAO;
    private final MenuView menuView;
    private final User userAccount;
    private final Map<String, DataOutputStream> onlineUserStreams;

    public ContactController(DataInputStream dis, DataOutputStream dos, ContactDAO contactDAO, UserDAO userDAO, MenuView menuView, User userAccount, Map<String, DataOutputStream> onlineUserStreams) {
        this.dis = dis;
        this.dos = dos;
        this.contactDAO = contactDAO;
        this.userDAO = userDAO;
        this.menuView = menuView;
        this.userAccount = userAccount;
        this.onlineUserStreams = onlineUserStreams;
    }

    public void handleMenu() throws IOException {
        boolean backToUserMenu = false;
        while (!backToUserMenu) {
            try {
                menuView.showContactsMenu();
                String choice = dis.readUTF();
                if (choice == null) throw new EOFException("Client disconnected during contacts menu.");
                choice = choice.trim();

                switch (choice) {
                    case "1": addContact(); menuView.promptContinue(); break;
                    case "2": deleteContact(); menuView.promptContinue(); break;
                    case "3": updateNickname(); menuView.promptContinue(); break;
                    case "4": listContacts(); menuView.promptContinue(); break;
                    case "5": backToUserMenu = true; break;
                    default: menuView.sendFeedback("Invalid choice (1-5).", AnsiColors.RED); menuView.promptContinue(); break;
                }
            } catch (EOFException | SocketException e) { throw e; }
            catch (IOException e) { menuView.sendFeedback("IO Error: " + e.getMessage(), AnsiColors.RED); System.err.println("ContactController IO Err: " + e); menuView.promptContinue(); }
            catch (Exception e) { menuView.sendFeedback("Error: " + e.getMessage(), AnsiColors.RED); System.err.println("ContactController Err: " + e); e.printStackTrace(); menuView.promptContinue(); }
        }
    }

    private void addContact() throws IOException {
        dos.writeUTF("\nEmail of contact to add:"); dos.flush();
        String email = dis.readUTF();
        if (email == null || !ValidationUtils.isValidEmail(email.trim())) { menuView.sendFeedback("Format of email incorrect.", AnsiColors.RED); return; }
        email = email.trim().toLowerCase();

        if (email.equalsIgnoreCase(userAccount.getEmail())) { menuView.sendFeedback("You cannot add.", AnsiColors.YELLOW); return; }

        int contactUserId = userDAO.getUserIdByEmail(email);
        if (contactUserId == -1) { menuView.sendFeedback("User '" + email + "' not found.", AnsiColors.RED); return; }

        if(contactDAO.doesSpecificContactExist(userAccount.getId(), contactUserId)) {
            menuView.sendFeedback("You have already add '" + email + "' as contact.", AnsiColors.YELLOW);
            return;
        }
        dos.writeUTF("Nickname for '" + email + "':"); dos.flush();
        String nickname = dis.readUTF();
        if (nickname == null || nickname.trim().isEmpty()) { menuView.sendFeedback("Nickname couldn't be empty.", AnsiColors.RED); return; }
        nickname = nickname.trim();

        if (contactDAO.getUserIdByNickname(userAccount.getId(), nickname) != -1) { menuView.sendFeedback("Nickname '" + nickname + "' already used.", AnsiColors.RED); return; }

        boolean success = contactDAO.addContact(userAccount.getId(), contactUserId, nickname);
        if (success) menuView.sendFeedback("Contact '" + nickname + "' (" + email + ") added !", AnsiColors.GREEN);
        else menuView.sendFeedback("Failed adding contact.", AnsiColors.RED);
    }

    private void deleteContact() throws IOException {
        dos.writeUTF("\nNickname of contact to delete:"); dos.flush();
        String nickname = dis.readUTF();
        if (nickname == null || nickname.trim().isEmpty()) { menuView.sendFeedback("Nickname incorrect.", AnsiColors.RED); return; }
        nickname = nickname.trim();

        int contactUserId = contactDAO.getUserIdByNickname(userAccount.getId(), nickname);
        if (contactUserId == -1) { menuView.sendFeedback("Nickname '" + nickname + "' not found.", AnsiColors.RED); return; }

        dos.writeUTF(AnsiColors.YELLOW + "Delete '" + nickname + "'? (yes/no):" + AnsiColors.RESET); dos.flush();
        String confirmation = dis.readUTF();
        if (confirmation == null || !confirmation.trim().equalsIgnoreCase("yes")) { menuView.sendFeedback("Deleted canceled.", AnsiColors.BLUE); return; }

        boolean success = contactDAO.deleteContact(userAccount.getId(), contactUserId);
        if(success) menuView.sendFeedback("Contact '" + nickname + "' deleted.", AnsiColors.GREEN);
        else menuView.sendFeedback("Erreur suppression.", AnsiColors.RED);
    }

    private void updateNickname() throws IOException {
        dos.writeUTF("\nCurrent nickname of contact:"); dos.flush();
        String oldNickname = dis.readUTF();
        if (oldNickname == null || oldNickname.trim().isEmpty()) { menuView.sendFeedback("Old nickname incorrect.", AnsiColors.RED); return; }
        oldNickname = oldNickname.trim();

        int contactUserId = contactDAO.getUserIdByNickname(userAccount.getId(), oldNickname);
        if (contactUserId == -1) { menuView.sendFeedback("Contact '" + oldNickname + "' not found.", AnsiColors.RED); return; }

        dos.writeUTF("New nickname:"); dos.flush();
        String newNickname = dis.readUTF();
        if (newNickname == null || newNickname.trim().isEmpty()) { menuView.sendFeedback("New nickname empty.", AnsiColors.RED); return; }
        newNickname = newNickname.trim();


        int existingId = contactDAO.getUserIdByNickname(userAccount.getId(), newNickname);
        if(existingId != -1 && existingId != contactUserId) {
            menuView.sendFeedback("Nickname '" + newNickname + "' already used.", AnsiColors.RED); return;
        }

        boolean success = contactDAO.updateNickname(userAccount.getId(), contactUserId, newNickname);
        if(success) menuView.sendFeedback("Update of nickname '" + newNickname + "'.", AnsiColors.GREEN);
        else menuView.sendFeedback("Error of update.", AnsiColors.RED);
    }

    private void listContacts() throws IOException {
            List<Contact> contacts = contactDAO.getContacts(userAccount.getId());
            if (contacts.isEmpty()) { dos.writeUTF("\nNo contact added.\r\n"); dos.flush(); return; }
            StringBuilder sb = new StringBuilder("\r\n--- Your contacts ---\r\n");
            for (Contact c : contacts) {
                String email = userDAO.getEmailById(c.getContactUserId());
                if (email != null) {
                    String status = onlineUserStreams.containsKey(email.toLowerCase()) ? AnsiColors.GREEN+"[Online]" : AnsiColors.RED+"[Offline]";
                    sb.append(String.format(" %s (%s) %s%n", AnsiColors.YELLOW+c.getNickname()+AnsiColors.RESET, AnsiColors.CYAN+email+AnsiColors.RESET, status+AnsiColors.RESET));
                } else {
                    sb.append(String.format(" %s (%s) [%s]%n", AnsiColors.YELLOW+c.getNickname()+AnsiColors.RESET, AnsiColors.GRAY+"Email not found"+AnsiColors.RESET, AnsiColors.GRAY+"not found"+AnsiColors.RESET));
                }
            }
            sb.append("-------------------------\r\n");
            dos.writeUTF(sb.toString()); dos.flush();
        }


}