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
        if (userAccount == null) { menuView.sendFeedback("Error: User not loaded.", AnsiColors.RED); return; }

        String newName = null, newEmail = null, newPassword = null, confirmNewPassword = null;
        boolean nameUpdated = false, emailUpdated = false, passwordUpdated = false, errorOccurred = false;
        StringBuilder feedback = new StringBuilder("\n--- Update Results ---\n");
        String oldEmailMapKey = userAccount.getEmail().toLowerCase();
        String newEmailMapKey = null;

        try {
            dos.writeUTF(AnsiColors.ANSI_CLS);
            dos.writeUTF(AnsiColors.BLUE + "====== Profile Update ======" + AnsiColors.RESET);
            dos.writeUTF("Current name: " + AnsiColors.YELLOW + userAccount.getName() + AnsiColors.RESET);
            dos.writeUTF("New name (Enter to skip):"); dos.flush();
            newName = dis.readUTF();

            dos.writeUTF("\nCurrent email: " + AnsiColors.YELLOW + userAccount.getEmail() + AnsiColors.RESET);
            dos.writeUTF("New email (Enter to skip):"); dos.flush();
            newEmail = dis.readUTF();

            dos.writeUTF("\nNew password (Enter to skip):"); dos.flush();
            newPassword = dis.readUTF();
            if (newPassword != null && !newPassword.isEmpty()){
                dos.writeUTF("Confirm new password:"); dos.flush();
                confirmNewPassword = dis.readUTF();
                if(confirmNewPassword == null) throw new EOFException("Client disconnected during password confirm.");
            }


            if (newName != null && !newName.trim().isEmpty() && !newName.trim().equals(userAccount.getName())) {
                try { userDAO.updateName(userAccount.getId(), newName.trim()); userAccount.setName(newName.trim()); nameUpdated = true; feedback.append(AnsiColors.GREEN+"Name updated.\n"+AnsiColors.RESET); }
                catch (Exception e) { errorOccurred = true; feedback.append(AnsiColors.RED+"Name error: ").append(e.getMessage()).append("\n"+AnsiColors.RESET); }
            }


            if (newEmail != null && !newEmail.trim().isEmpty()) {
                String trimmedNewEmail = newEmail.trim().toLowerCase();
                if (!trimmedNewEmail.equalsIgnoreCase(userAccount.getEmail())) {
                    if (ValidationUtils.isValidEmail(trimmedNewEmail)) {
                        if (!userDAO.userExists(trimmedNewEmail)) {
                            try { userDAO.updateEmail(userAccount.getId(), trimmedNewEmail); userAccount.setEmail(trimmedNewEmail); newEmailMapKey = trimmedNewEmail; emailUpdated = true; feedback.append(AnsiColors.GREEN+"Email updated.\n"+AnsiColors.RESET); }
                            catch (Exception e) { errorOccurred = true; feedback.append(AnsiColors.RED+"Email DB error: ").append(e.getMessage()).append("\n"+AnsiColors.RESET); }
                        } else { errorOccurred = true; feedback.append(AnsiColors.RED+"Email already taken.\n"+AnsiColors.RESET); }
                    } else { errorOccurred = true; feedback.append(AnsiColors.RED+"Invalid email format.\n"+AnsiColors.RESET); }
                }
            }


            if (newPassword != null && !newPassword.isEmpty()) {
                if (newPassword.equals(confirmNewPassword)) {
                    if (ValidationUtils.isValidPassword(newPassword)) {
                        try { userDAO.updatePassword(userAccount.getId(), newPassword); userAccount.setPassword(newPassword); passwordUpdated = true; feedback.append(AnsiColors.GREEN+"Password updated.\n"+AnsiColors.RESET); }
                        catch (Exception e) { errorOccurred = true; feedback.append(AnsiColors.RED+"Password DB error: ").append(e.getMessage()).append("\n"+AnsiColors.RESET); }
                    } else { errorOccurred = true; feedback.append(AnsiColors.RED+"Password not secure enough.\n"+AnsiColors.RESET); }
                } else { errorOccurred = true; feedback.append(AnsiColors.RED+"Password confirmation failed.\n"+AnsiColors.RESET); }
            }


            if (emailUpdated && newEmailMapKey != null) {
                if (onlineUserStreams.containsKey(oldEmailMapKey)) { DataOutputStream stream = onlineUserStreams.remove(oldEmailMapKey); if (stream != null) onlineUserStreams.put(newEmailMapKey, stream); System.out.println("[PROFILE] Map key updated: "+oldEmailMapKey+" -> "+newEmailMapKey); }
                else { System.err.println("[PROFILE WARN] Old email "+oldEmailMapKey+" not in map."); }
            }


            if (!nameUpdated && !emailUpdated && !passwordUpdated && !errorOccurred) { feedback.append(AnsiColors.GRAY+"No changes made.\n"+AnsiColors.RESET); }
            else if (!errorOccurred) { feedback.append(AnsiColors.GREEN+"Profile updated successfully!\n"+AnsiColors.RESET); }
            dos.writeUTF(feedback.toString()); dos.flush();

        } catch (EOFException | SocketException e) {
            throw e;
        } catch (IOException e) {
            menuView.sendFeedback("Profile communication error: "+e.getMessage(), AnsiColors.RED);
            throw e;
        } catch (Exception e) {
            menuView.sendFeedback("Profile update error: "+e.getMessage(), AnsiColors.RED);
            e.printStackTrace();
        } finally {
            menuView.promptContinue();
        }
    }
}