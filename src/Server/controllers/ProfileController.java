package Server.controllers;

import Server.dao.UserDAO;
import Server.entities.User;
import Server.utils.AnsiColors;
import Server.utils.ValidationUtils;
import Server.views.MenuView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Map;

public class ProfileController {

    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final UserDAO userDAO;
    private final MenuView menuView;
    private final User userAccount;
    private final Map<String, DataOutputStream> onlineUserStreams;

    public ProfileController(DataInputStream dis, DataOutputStream dos, UserDAO userDAO, MenuView menuView, User userAccount, Map<String, DataOutputStream> onlineUserStreams) {
        this.dis = dis;
        this.dos = dos;
        this.userDAO = userDAO;
        this.menuView = menuView;
        this.userAccount = userAccount;
        this.onlineUserStreams = onlineUserStreams;
    }

    public void updateProfile() throws IOException {
        if (userAccount == null) { menuView.sendFeedback("Erreur: Utilisateur non chargé.", AnsiColors.RED); return; }

        String newName = null, newEmail = null, newPassword = null, confirmNewPassword = null;
        boolean nameUpdated = false, emailUpdated = false, passwordUpdated = false, errorOccurred = false;
        StringBuilder feedback = new StringBuilder("\n--- Résultat Mise à Jour ---\n");
        String oldEmailMapKey = userAccount.getEmail().toLowerCase();
        String newEmailMapKey = null;

        try {
            dos.writeUTF(AnsiColors.ANSI_CLS);
            dos.writeUTF(AnsiColors.BLUE + "====== MàJ Profil ======" + AnsiColors.RESET);
            dos.writeUTF("Nom actuel: " + AnsiColors.YELLOW + userAccount.getName() + AnsiColors.RESET);
            dos.writeUTF("Nouveau nom (Entrée pour ignorer):"); dos.flush();
            newName = dis.readUTF();

            dos.writeUTF("\nEmail actuel: " + AnsiColors.YELLOW + userAccount.getEmail() + AnsiColors.RESET);
            dos.writeUTF("Nouvel email (Entrée pour ignorer):"); dos.flush();
            newEmail = dis.readUTF();

            dos.writeUTF("\nNouveau mot de passe (Entrée pour ignorer):"); dos.flush();
            newPassword = dis.readUTF();
            if (newPassword != null && !newPassword.isEmpty()){
                dos.writeUTF("Confirmer nouveau mot de passe:"); dos.flush();
                confirmNewPassword = dis.readUTF();
                if(confirmNewPassword == null) throw new EOFException("Client disconnected during password confirm.");
            }

            // Traitement Nom
            if (newName != null && !newName.trim().isEmpty() && !newName.trim().equals(userAccount.getName())) {
                try { userDAO.updateName(userAccount.getId(), newName.trim()); userAccount.setName(newName.trim()); nameUpdated = true; feedback.append(AnsiColors.GREEN+"Nom MàJ.\n"+AnsiColors.RESET); }
                catch (Exception e) { errorOccurred = true; feedback.append(AnsiColors.RED+"Erreur Nom: ").append(e.getMessage()).append("\n"+AnsiColors.RESET); }
            }

            // Traitement Email
            if (newEmail != null && !newEmail.trim().isEmpty()) {
                String trimmedNewEmail = newEmail.trim().toLowerCase();
                if (!trimmedNewEmail.equalsIgnoreCase(userAccount.getEmail())) {
                    if (ValidationUtils.isValidEmail(trimmedNewEmail)) {
                        if (!userDAO.userExists(trimmedNewEmail)) {
                            try { userDAO.updateEmail(userAccount.getId(), trimmedNewEmail); userAccount.setEmail(trimmedNewEmail); newEmailMapKey = trimmedNewEmail; emailUpdated = true; feedback.append(AnsiColors.GREEN+"Email MàJ.\n"+AnsiColors.RESET); }
                            catch (Exception e) { errorOccurred = true; feedback.append(AnsiColors.RED+"Erreur Email DB: ").append(e.getMessage()).append("\n"+AnsiColors.RESET); }
                        } else { errorOccurred = true; feedback.append(AnsiColors.RED+"Email déjà pris.\n"+AnsiColors.RESET); }
                    } else { errorOccurred = true; feedback.append(AnsiColors.RED+"Format Email invalide.\n"+AnsiColors.RESET); }
                }
            }

            // Traitement Mot de passe
            if (newPassword != null && !newPassword.isEmpty()) {
                if (newPassword.equals(confirmNewPassword)) {
                    if (ValidationUtils.isValidPassword(newPassword)) {
                        try { userDAO.updatePassword(userAccount.getId(), newPassword); userAccount.setPassword(newPassword); passwordUpdated = true; feedback.append(AnsiColors.GREEN+"Mdp MàJ.\n"+AnsiColors.RESET); }
                        catch (Exception e) { errorOccurred = true; feedback.append(AnsiColors.RED+"Erreur Mdp DB: ").append(e.getMessage()).append("\n"+AnsiColors.RESET); }
                    } else { errorOccurred = true; feedback.append(AnsiColors.RED+"Mdp non sécurisé.\n"+AnsiColors.RESET); }
                } else { errorOccurred = true; feedback.append(AnsiColors.RED+"Confirmation Mdp échouée.\n"+AnsiColors.RESET); }
            }

            // MàJ Map si email changé
            if (emailUpdated && newEmailMapKey != null) {
                if (onlineUserStreams.containsKey(oldEmailMapKey)) { DataOutputStream stream = onlineUserStreams.remove(oldEmailMapKey); if (stream != null) onlineUserStreams.put(newEmailMapKey, stream); System.out.println("[PROFILE] Map key updated: "+oldEmailMapKey+" -> "+newEmailMapKey); }
                else { System.err.println("[PROFILE WARN] Old email "+oldEmailMapKey+" not in map."); /* Peut-être ajouter la nouvelle clé quand même? onlineUserStreams.put(newEmailMapKey, this.dos); */}
            }

            // Feedback final
            if (!nameUpdated && !emailUpdated && !passwordUpdated && !errorOccurred) { feedback.append(AnsiColors.GRAY+"Aucune modification.\n"+AnsiColors.RESET); }
            else if (!errorOccurred) { feedback.append(AnsiColors.GREEN+"Profil mis à jour !\n"+AnsiColors.RESET); }
            dos.writeUTF(feedback.toString()); dos.flush();

        } catch (EOFException | SocketException e) {
            throw e; // Remonter pour gestion déco par ClientHandler
        } catch (IOException e) {
            menuView.sendFeedback("Erreur communication profil: "+e.getMessage(), AnsiColors.RED);
            throw e; // Remonter
        } catch (Exception e) {
            menuView.sendFeedback("Erreur mise à jour profil: "+e.getMessage(), AnsiColors.RED);
            e.printStackTrace(); // Log serveur
        } finally {
            menuView.promptContinue(); // Toujours proposer de continuer après la tentative
        }
    }
}