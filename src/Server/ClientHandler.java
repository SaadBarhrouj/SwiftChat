package Server;

import Client.UserAccount;
import Database.MessageDAO;
import Database.UserDAO;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;

public class ClientHandler extends Thread {
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final Socket commthread;
    private boolean Auth;
    private UserAccount userAccount;
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
                        case "c":  // Exit case
                            this.dos.writeUTF("Goodbye!");
                            return;
                        default:
                            this.dos.writeUTF("Invalid choice, please try again");
                    }
                } while (!Auth);



                if (Auth) {
                    receiveAndDeleteMessages();
                }
            }
        } catch (IOException e) {
            System.err.println("Error in ClientHandler: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                // Ensure resources are closed
                if (dis != null) dis.close();
                if (dos != null) dos.close();
                if (commthread != null && !commthread.isClosed()) commthread.close();
            } catch (IOException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    private boolean login(String login, String password) {
        try {
            var rs = messageDAO.getUserByEmailAndPassword(login, password);
            if (rs.next()) {
                dos.writeUTF("Login Successful");
                userAccount = new UserAccount(rs.getInt("user_id"), rs.getString("email"), rs.getString("name"), dos, dis);
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

    private void receiveAndDeleteMessages() {
        try {
            var messages = messageDAO.getMessagesForUser(userAccount.getEmail());
            for (var msg : messages) {
                if (msg.getMessageType().equals("text")) {
                    dos.writeUTF(msg.getMessageType() + "Message From " + msg.getSenderEmail() + "At" + msg.getDate() + ":" + msg.getContent());
                } else {
                    dos.writeUTF(msg.getMessageType() + "Message From " + msg.getSenderEmail() + "At" + msg.getDate() + ":" + msg.getFileName());
                    byte[] bytes = Base64.getDecoder().decode(msg.getContent());
                    dos.writeInt(bytes.length);
                    dos.write(bytes);
                }
            }
            messageDAO.deleteMessagesForUser(userAccount.getEmail());
        } catch (SQLException | IOException e) {
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
