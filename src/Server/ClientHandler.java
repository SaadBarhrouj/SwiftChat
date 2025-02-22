package Server;

import Entities.User;
import Database.MessageDAO;
import Database.UserDAO;

import java.io.*;
        import java.net.Socket;
import java.sql.SQLException;
import java.util.Base64;

public class ClientHandler extends Thread {
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final Socket commthread;
    private boolean Auth;
    private User userAccount;
    private UserDAO userDAO;
    private MessageDAO messageDAO;

    public ClientHandler(Socket s, DataInputStream diss, DataOutputStream doss) {
        this.commthread = s;
        this.dis = diss;
        this.dos = doss;
        this.Auth = false;
        this.userDAO = new UserDAO();
        this.messageDAO = new MessageDAO();
    }

    @Override
    public void run() {
        try {
            while (true) {
                String choice = "";
                do {
                    this.showMenu();
                    choice = this.dis.readLine();  // Reading client choice

                    String name, email, password;
                    switch (choice) {
                        case "a":  // Register case
                            this.dos.writeUTF("Please enter your name:");
                            name = this.dis.readLine();
                            this.dos.writeUTF("Please enter your email:");
                            email = this.dis.readLine();
                            this.dos.writeUTF("Please enter your password:");
                            password = this.dis.readLine();
                            register(name, email, password);
                            break;
                        case "b":  // Login case
                            this.dos.writeUTF("Please enter your email:");
                            email = this.dis.readLine();
                            this.dos.writeUTF("Please enter your password:");
                            password = this.dis.readLine();
                            Auth = login(email, password);
                            break;
                        default:
                            this.dos.writeUTF("Invalid choice, please try again");
                    }
                } while (!Auth);

                if (Auth) {
                    userMenu();
                }
            }
        } catch (IOException e) {
            System.err.println("Error in ClientHandler: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (dis != null) dis.close();
                if (dos != null) dos.close();
                if (commthread != null && !commthread.isClosed()) commthread.close();
            } catch (IOException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    private void userMenu() throws IOException {
        String choice;
        do {
            this.dos.writeUTF("\n====== User Menu ======\n");
            this.dos.writeUTF("a. Send a message\n");
            this.dos.writeUTF("b. View received messages\n");
            this.dos.writeUTF("c. Logout\n");
            this.dos.writeUTF("=======================\n");
            this.dos.writeUTF("Please enter your choice:");

            choice = this.dis.readLine();
            switch (choice) {
                case "a":
                    sendMessage();
                    break;
                case "b":
                    receiveAndDeleteMessages();
                    break;
                case "c":
                    userAccount.disconnect();
                    this.dos.writeUTF("Logging out...");
                    Auth = false;
                    break;
                default:
                    this.dos.writeUTF("Invalid choice, please try again");
            }
        } while (Auth);
    }

    private boolean login(String login, String password) {
        try {
            var rs = messageDAO.getUserByEmailAndPassword(login, password);
            if (rs.next()) {
                dos.writeUTF("Login Successful");
                userAccount = new User(rs.getInt("user_id"), rs.getString("email"), rs.getString("name"), dos, dis);
                return true;
            }
            dos.writeUTF("Login Failed");
            return false;
        } catch (SQLException | IOException e) {
            System.err.println("Login error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean register(String name, String email, String password) {
        try {
            if (userDAO.userExists(email)) {
                dos.writeUTF("Email already used");
                return false;
            } else {
                if (userDAO.insertUser(name, email, password)) {
                    dos.writeUTF("Registration Successful");
                    return true;
                }
            }
        } catch (SQLException | IOException e) {
            System.err.println("Registration error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private void sendMessage() throws IOException {
        this.dos.writeUTF("Enter recipient email:");
        String recipient = this.dis.readLine();
        this.dos.writeUTF("Enter your message:");
        String message = this.dis.readLine();

        boolean sent = userAccount.sendMessage(recipient, "text:" + message);
        if (sent) {
            this.dos.writeUTF("Message sent successfully.");
        } else {
            this.dos.writeUTF("Failed to send message.");
        }
    }

    private void receiveAndDeleteMessages() {
        try {
            var messages = messageDAO.getMessagesForUser(userAccount.getEmail());
            for (var msg : messages) {
                dos.writeUTF("From " + msg.getSenderEmail() + " at " + msg.getDate() + ": " + msg.getMessage());
            }
            messageDAO.deleteMessagesForUser(userAccount.getEmail());
        } catch (IOException e) {
            System.err.println("Error while receiving or deleting messages: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void showMenu() throws IOException {
        try {
            this.dos.writeUTF("\n====== Main Menu ======\n");
            this.dos.writeUTF("\na. Create an account");
            this.dos.writeUTF("\nb. Log in");
            this.dos.writeUTF("\nc. Quit");
            this.dos.writeUTF("\n=======================");
            this.dos.writeUTF("\nPlease enter your choice:");
        } catch (IOException e) {
            System.err.println("Error showing menu: " + e.getMessage());
            throw e;  // Re-throw to be caught in the calling method
        }
    }

}
