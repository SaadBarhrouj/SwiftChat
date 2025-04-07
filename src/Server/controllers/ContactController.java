package Server.controllers;

import Server.dao.ContactDAO;
import Server.dao.UserDAO;
import Server.entities.Contact;
import Server.entities.User;
import Server.utils.AnsiColors;
import Server.utils.ValidationUtils;
import Server.views.MenuView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
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
        dos.writeUTF("\nEmail du contact à ajouter:"); dos.flush();
        String email = dis.readUTF();
        if (email == null || !ValidationUtils.isValidEmail(email.trim())) { menuView.sendFeedback("Format d'email invalide.", AnsiColors.RED); return; }
        email = email.trim().toLowerCase();

        if (email.equalsIgnoreCase(userAccount.getEmail())) { menuView.sendFeedback("Vous ne pouvez pas vous ajouter.", AnsiColors.YELLOW); return; }

        int contactUserId = userDAO.getUserIdByEmail(email);
        if (contactUserId == -1) { menuView.sendFeedback("Utilisateur '" + email + "' non trouvé.", AnsiColors.RED); return; }

        // ***** UTILISATION DE LA NOUVELLE MÉTHODE DAO *****
        if(contactDAO.doesSpecificContactExist(userAccount.getId(), contactUserId)) {
            menuView.sendFeedback("Vous avez déjà ajouté '" + email + "' comme contact.", AnsiColors.YELLOW);
            return;
        }
        // ***** FIN MODIFICATION *****

        dos.writeUTF("Surnom pour '" + email + "':"); dos.flush();
        String nickname = dis.readUTF();
        if (nickname == null || nickname.trim().isEmpty()) { menuView.sendFeedback("Le surnom ne peut pas être vide.", AnsiColors.RED); return; }
        nickname = nickname.trim();

        if (contactDAO.getUserIdByNickname(userAccount.getId(), nickname) != -1) { menuView.sendFeedback("Surnom '" + nickname + "' déjà pris.", AnsiColors.RED); return; }

        boolean success = contactDAO.addContact(userAccount.getId(), contactUserId, nickname);
        if (success) menuView.sendFeedback("Contact '" + nickname + "' (" + email + ") ajouté !", AnsiColors.GREEN);
        else menuView.sendFeedback("Échec ajout contact.", AnsiColors.RED);
    }

    private void deleteContact() throws IOException {
        dos.writeUTF("\nSurnom EXACT du contact à supprimer:"); dos.flush();
        String nickname = dis.readUTF();
        if (nickname == null || nickname.trim().isEmpty()) { menuView.sendFeedback("Surnom invalide.", AnsiColors.RED); return; }
        nickname = nickname.trim();

        int contactUserId = contactDAO.getUserIdByNickname(userAccount.getId(), nickname);
        if (contactUserId == -1) { menuView.sendFeedback("Surnom '" + nickname + "' non trouvé.", AnsiColors.RED); return; }

        dos.writeUTF(AnsiColors.YELLOW + "Supprimer '" + nickname + "'? (oui/non):" + AnsiColors.RESET); dos.flush();
        String confirmation = dis.readUTF();
        if (confirmation == null || !confirmation.trim().equalsIgnoreCase("oui")) { menuView.sendFeedback("Suppression annulée.", AnsiColors.BLUE); return; }

        boolean success = contactDAO.deleteContact(userAccount.getId(), contactUserId);
        if(success) menuView.sendFeedback("Contact '" + nickname + "' supprimé.", AnsiColors.GREEN);
        else menuView.sendFeedback("Erreur suppression.", AnsiColors.RED);
    }

    private void updateNickname() throws IOException {
        dos.writeUTF("\nSurnom ACTUEL du contact:"); dos.flush();
        String oldNickname = dis.readUTF();
        if (oldNickname == null || oldNickname.trim().isEmpty()) { menuView.sendFeedback("Ancien surnom invalide.", AnsiColors.RED); return; }
        oldNickname = oldNickname.trim();

        int contactUserId = contactDAO.getUserIdByNickname(userAccount.getId(), oldNickname);
        if (contactUserId == -1) { menuView.sendFeedback("Contact '" + oldNickname + "' non trouvé.", AnsiColors.RED); return; }

        dos.writeUTF("Nouveau surnom:"); dos.flush();
        String newNickname = dis.readUTF();
        if (newNickname == null || newNickname.trim().isEmpty()) { menuView.sendFeedback("Nouveau surnom vide.", AnsiColors.RED); return; }
        newNickname = newNickname.trim();

        // Vérifier si le nouveau surnom est déjà utilisé pour un AUTRE contact
        int existingId = contactDAO.getUserIdByNickname(userAccount.getId(), newNickname);
        if(existingId != -1 && existingId != contactUserId) {
            menuView.sendFeedback("Le surnom '" + newNickname + "' est déjà utilisé.", AnsiColors.RED); return;
        }

        boolean success = contactDAO.updateNickname(userAccount.getId(), contactUserId, newNickname);
        if(success) menuView.sendFeedback("Surnom mis à jour pour '" + newNickname + "'.", AnsiColors.GREEN);
        else menuView.sendFeedback("Échec mise à jour.", AnsiColors.RED);
    }

    private void listContacts() throws IOException {
        List<Contact> contacts = contactDAO.getContacts(userAccount.getId()); // Ne liste que ceux ajoutés par moi
        if (contacts.isEmpty()) { dos.writeUTF("\nAucun contact ajouté.\r\n"); dos.flush(); return; }
        StringBuilder sb = new StringBuilder("\r\n--- Vos Contacts Ajoutés ---\r\n");
        for (Contact c : contacts) {
            String email = userDAO.getEmailById(c.getContactUserId());
            if (email != null) {
                String status = onlineUserStreams.containsKey(email.toLowerCase()) ? AnsiColors.GREEN+"[En ligne]" : AnsiColors.RED+"[Hors ligne]";
                sb.append(String.format(" %s (%s) %s%n", AnsiColors.YELLOW+c.getNickname()+AnsiColors.RESET, AnsiColors.CYAN+email+AnsiColors.RESET, status+AnsiColors.RESET));
            } else {
                sb.append(String.format(" %s (%s) [%s]%n", AnsiColors.YELLOW+c.getNickname()+AnsiColors.RESET, AnsiColors.GRAY+"Email inconnu"+AnsiColors.RESET, AnsiColors.GRAY+"Inconnu"+AnsiColors.RESET));
            }
        }
        sb.append("-------------------------\r\n");
        dos.writeUTF(sb.toString()); dos.flush();
    }
}