package Server.views;

import Server.utils.AnsiColors;

import java.io.DataOutputStream;
import java.io.IOException;

public class HelpView {

    private final DataOutputStream dos;

    public HelpView(DataOutputStream dos) {
        this.dos = dos;
    }

    public void displayGeneralHelp() throws IOException {
        StringBuilder help = new StringBuilder();
        help.append(AnsiColors.ANSI_CLS);
        help.append(AnsiColors.YELLOW + "====== SwiftChat Help & Information ======" + AnsiColors.RESET + "\r\n\r\n");
        help.append(AnsiColors.CYAN + "SwiftChat" + AnsiColors.RESET + " is a command-line chat application allowing:\r\n");
        help.append("- User registration and login.\r\n");
        help.append("- Managing contacts (add, list, delete, nickname).\r\n");
        help.append("- Private one-on-one conversations.\r\n");
        help.append("- Group creation and management (add/remove members).\r\n");
        help.append("- Group conversations.\r\n");
        help.append("- File sharing in private and group chats (upload, view, download).\r\n");
        help.append("- Profile updates (name, email, password).\r\n");
        help.append("\r\n");
        help.append(AnsiColors.MAGENTA + "Navigation:" + AnsiColors.RESET + "\r\n");
        help.append("- Follow the on-screen menus (enter the number or letter corresponding to your choice).\r\n");
        help.append("- In chats, type your message and press Enter.\r\n");
        help.append("- Use specific commands like " + AnsiColors.GREEN + "'view ID'" + AnsiColors.RESET + ", " + AnsiColors.GREEN + "'download ID'" + AnsiColors.RESET + ", " + AnsiColors.GREEN + "'delete ID'" + AnsiColors.RESET + " within chats.\r\n");
        help.append("- Use " + AnsiColors.GREEN + "'retour'" + AnsiColors.RESET + " to go back from chats or sub-menus.\r\n");
        help.append("- Use " + AnsiColors.GREEN + "'upload /path/to/your/file.txt'" + AnsiColors.RESET + " (on your client) to initiate file sending.\r\n");
        help.append("\r\n");
        help.append(AnsiColors.YELLOW + "==========================================" + AnsiColors.RESET + "\r\n");

        dos.writeUTF(help.toString());
        dos.flush();

    }

    public void showChatHelp() throws IOException {
        StringBuilder help = new StringBuilder();
        help.append(AnsiColors.YELLOW + "\n====== Chat Commands Help ======" + AnsiColors.RESET + "\r\n");
        help.append(AnsiColors.GREEN + "message" + AnsiColors.RESET + "       - Type your message and press Enter to send.\r\n");
        help.append(AnsiColors.GREEN + "upload /path/to/file" + AnsiColors.RESET + " - Type this on *your client* to start sending a file.\r\n");
        help.append(AnsiColors.GREEN + "view <ID>" + AnsiColors.RESET + "    - View a received file (if text/image supported by client).\r\n");
        help.append(AnsiColors.GREEN + "download <ID>" + AnsiColors.RESET + " - Download a received file to your client.\r\n");
        help.append(AnsiColors.GREEN + "delete <ID>" + AnsiColors.RESET + "  - Delete a message *you* sent.\r\n");
        help.append(AnsiColors.GREEN + "retour" + AnsiColors.RESET + "        - Exit the current chat session.\r\n");
        help.append(AnsiColors.GREEN + "help" + AnsiColors.RESET + "          - Show this help message again.\r\n");
        help.append(AnsiColors.YELLOW + "===============================\n" + AnsiColors.RESET);

        dos.writeUTF(help.toString());
        dos.flush();

    }

    public void showGroupChatHelp() throws IOException {

        showChatHelp();

    }
}