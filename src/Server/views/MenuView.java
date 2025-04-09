package Server.views;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import Server.utils.AnsiColors;

public class MenuView {

    private final DataInputStream dis;
    private final DataOutputStream dos;

    public MenuView(DataInputStream dis, DataOutputStream dos) {
        this.dis = dis;
        this.dos = dos;
    }

    public void showMainMenu() throws IOException {

        dos.writeUTF("\r\n\r\n");
        StringBuilder menu = new StringBuilder();
        menu.append(AnsiColors.WHITE).append("==========================================\r\n");
        menu.append("      >>> Welcome to SwiftChat! <<<\r\n");
        menu.append("==========================================\r\n\r\n");
        menu.append(AnsiColors.YELLOW).append("           --- MAIN MENU ---\r\n\r\n");
        menu.append(AnsiColors.GREEN).append("    a.  Sign Up\r\n");
        menu.append(AnsiColors.CYAN).append("    b.  Log In\r\n");
        menu.append(AnsiColors.MAGENTA).append("    c.  Help / Information\r\n");
        menu.append(AnsiColors.RESET).append("\r\n");
        menu.append(AnsiColors.BLUE).append("------------------------------------------\r\n");
        menu.append("\r\n");
        menu.append(AnsiColors.CYAN).append("Your choice (a, b, or c): ").append(AnsiColors.RESET);

        this.dos.writeUTF(menu.toString());
        this.dos.flush();
    }

    public void showUserMenu() throws IOException {

        StringBuilder menu = new StringBuilder();
        menu.append("\r\n" + AnsiColors.YELLOW + "====== USER MENU ======" + AnsiColors.RESET + "\r\n");
        menu.append(AnsiColors.GREEN).append("1. Manage Contacts\r\n");
        menu.append(AnsiColors.CYAN).append("2. Private Chat / Files\r\n");
        menu.append(AnsiColors.MAGENTA).append("3. Manage Groups\r\n");
        menu.append(AnsiColors.BLUE).append("4. Update Profile\r\n");
        menu.append(AnsiColors.RED).append("5. Log Out\r\n");
        menu.append(AnsiColors.YELLOW).append("=======================" + AnsiColors.RESET + "\r\n");
        menu.append(AnsiColors.CYAN).append("Please enter your choice: ").append(AnsiColors.RESET);
        dos.writeUTF(menu.toString());
        dos.flush();
    }

    public void showContactsMenu() throws IOException {

        StringBuilder menu = new StringBuilder();
        menu.append("\r\n" + AnsiColors.GREEN + "====== CONTACTS MENU ======" + AnsiColors.RESET + "\r\n");
        menu.append(AnsiColors.YELLOW).append("1. Add a Contact\r\n");
        menu.append(AnsiColors.RED).append("2. Delete a Contact\r\n");
        menu.append(AnsiColors.CYAN).append("3. Update Contact Nickname\r\n");
        menu.append(AnsiColors.MAGENTA).append("4. List Contacts\r\n");
        menu.append(AnsiColors.BLUE).append("5. Return to Main Menu\r\n"); // Retour au menu User
        menu.append(AnsiColors.GREEN).append("==========================" + AnsiColors.RESET + "\r\n");
        menu.append(AnsiColors.CYAN).append("Please enter your choice: ").append(AnsiColors.RESET);
        dos.writeUTF(menu.toString());
        dos.flush();
    }

    public void showGroupsMenu() throws IOException {

        StringBuilder menu = new StringBuilder();
        menu.append("\r\n" + AnsiColors.MAGENTA + "====== GROUP MENU ======" + AnsiColors.RESET + "\r\n");
        menu.append(AnsiColors.YELLOW).append("1. Create a Group\r\n");
        menu.append(AnsiColors.GREEN).append("2. Join a Group (by name)\r\n");
        menu.append(AnsiColors.CYAN).append("3. Add Member (admin only)\r\n");
        menu.append(AnsiColors.RED).append("4. Remove Member (admin only)\r\n");
        menu.append(AnsiColors.BLUE).append("5. Show My Groups\r\n");
        menu.append(AnsiColors.WHITE).append("6. Show Group Members\r\n");
        menu.append(AnsiColors.YELLOW).append("7. Group Conversation\r\n");
        menu.append(AnsiColors.BLUE).append("8. Leave Group\r\n");
        menu.append(AnsiColors.MAGENTA).append("9. Return to User Menu\r\n"); // Retour au menu User
        menu.append(AnsiColors.CYAN).append("========================" + AnsiColors.RESET + "\r\n");
        menu.append(AnsiColors.YELLOW).append("Please enter your choice: ").append(AnsiColors.RESET);
        dos.writeUTF(menu.toString());
        dos.flush();
    }



    private void promptWithMessage(String message) throws IOException {
        dos.writeUTF(message);
        dos.flush();
        try {

            String dummyInput = dis.readUTF();
            if (dummyInput == null) {
                throw new EOFException("Client disconnected while waiting for prompt.");
            }
        } catch (EOFException | SocketException e) {
            System.err.println(AnsiColors.YELLOW + "[PROMPT] Client disconnected: " + e.getMessage() + AnsiColors.RESET);
            throw e;
        }
    }

    public void promptContinue() throws IOException {
        promptWithMessage(AnsiColors.GRAY + "\nPress Enter to continue..." + AnsiColors.RESET);
    }

    public void promptBeforeRetry(String message) throws IOException {
        promptWithMessage("\r\n" + message);
    }


    public void sendFeedback(String message, String color) throws IOException {
        String border = color + "-----------------------------------------" + AnsiColors.RESET;
        dos.writeUTF("\r\n" + border + "\r\n" + color + message + AnsiColors.RESET + "\r\n" + border + "\r\n");
        dos.flush();
    }


}