package Server.controllers;

import Server.dao.UserDAO;
import Server.entities.User;
import Server.utils.*;
import Server.views.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class AuthenticationController {

    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final UserDAO userDAO;
    private final MenuView menuView;
    private final HelpView helpView;
    private final Map<String, DataOutputStream> onlineUserStreams;

    public AuthenticationController(DataInputStream dis, DataOutputStream dos, UserDAO userDAO, MenuView menuView, HelpView helpView, Map<String, DataOutputStream> onlineUserStreams) {
        this.dis = dis;
        this.dos = dos;
        this.userDAO = userDAO;
        this.menuView = menuView;
        this.helpView = helpView;
        this.onlineUserStreams = onlineUserStreams;
    }

    public User handleAuthentication() throws IOException {
        User authenticatedUser = null;
        boolean isAuthenticated = false;
        String choice;

        do {
            menuView.showMainMenu();

            try {
                choice = this.dis.readUTF();
                if (choice == null) throw new EOFException("Input stream returned null during auth.");
            } catch (EOFException | SocketException e) {
                System.err.println(AnsiColors.YELLOW + "[AUTH] Client disconnected (" + e.getClass().getSimpleName() + ") waiting for menu choice." + AnsiColors.RESET);
                return null;
            }

            String trimmedChoice = choice.trim().toLowerCase();
            if (trimmedChoice.isEmpty()) continue;

            switch (trimmedChoice) {
                case "a":
                    try {
                        authenticatedUser = handleRegistration();
                        isAuthenticated = (authenticatedUser != null);
                        if (!isAuthenticated) {
                            menuView.promptBeforeRetry("Press Enter to try again...");
                        }
                    } catch (EOFException | SocketException e) {
                        System.err.println(AnsiColors.YELLOW + "[AUTH] Client disconnected (" + e.getClass().getSimpleName() + ") during registration." + AnsiColors.RESET);
                        return null;
                    }
                    break;

                case "b":
                    try {
                        authenticatedUser = handleLogin();
                        isAuthenticated = (authenticatedUser != null);
                        if (!isAuthenticated) {
                            menuView.promptBeforeRetry("Press Enter to try again...");
                        }
                    } catch (EOFException | SocketException e) {
                        System.err.println(AnsiColors.YELLOW + "[AUTH] Client disconnected (" + e.getClass().getSimpleName() + ") during login." + AnsiColors.RESET);
                        return null;
                    }
                    break;

                case "c":
                    try {
                        helpView.displayGeneralHelp();
                        menuView.promptBeforeRetry("Press Enter to return to the main menu...");
                    } catch (EOFException | SocketException e) {
                        System.err.println(AnsiColors.YELLOW + "[AUTH] Client disconnected (" + e.getClass().getSimpleName() + ") during help prompt." + AnsiColors.RESET);
                        return null;
                    }
                    break;

                default:
                    try {
                        menuView.sendFeedback("Invalid choice ('" + choice.trim() + "'). Select a, b, or c.", AnsiColors.RED);
                        menuView.promptBeforeRetry("Press Enter to try again...");
                    } catch (EOFException | SocketException e) {
                        System.err.println(AnsiColors.YELLOW + "[AUTH] Client disconnected (" + e.getClass().getSimpleName() + ") during invalid choice prompt." + AnsiColors.RESET);
                        return null;
                    }
                    break;
            }

        } while (!isAuthenticated);

        return authenticatedUser;
    }


    private User handleLogin() throws IOException {
        String email = null, password = null;

        try {
            dos.writeUTF(AnsiColors.GREEN + "\r\n--- Log In ---" + AnsiColors.RESET);
            dos.writeUTF(AnsiColors.CYAN + "Email:" + AnsiColors.RESET); dos.flush();
            email = dis.readUTF();
            dos.writeUTF(AnsiColors.CYAN + "Password:" + AnsiColors.RESET); dos.flush();
            password = dis.readUTF();
            if (email == null || password == null) throw new EOFException("Client disconnected during login input.");
            return login(email, password);
        } catch (EOFException | SocketException e) {
            throw e;
        }
    }

    private User handleRegistration() throws IOException {
        String name = null, email = null, password = null, confirmPassword = null;
        try {
            dos.writeUTF(AnsiColors.GREEN + "\r\n--- Sign Up ---" + AnsiColors.RESET);
            dos.writeUTF(AnsiColors.CYAN + "Name:" + AnsiColors.RESET); dos.flush(); name = dis.readUTF();
            dos.writeUTF(AnsiColors.CYAN + "Email:" + AnsiColors.RESET); dos.flush(); email = dis.readUTF();
            dos.writeUTF(AnsiColors.CYAN + "Password:" + AnsiColors.RESET); dos.flush(); password = dis.readUTF();
            dos.writeUTF(AnsiColors.CYAN + "Confirm Password:" + AnsiColors.RESET); dos.flush(); confirmPassword = dis.readUTF();
            if (name==null || email==null || password==null || confirmPassword==null) throw new EOFException("Client disconnected during registration input.");
            return register(name, email, password, confirmPassword);
        } catch (EOFException | SocketException e) {
            throw e;
        }
    }

    private User login(String email, String password) throws IOException {
        String emailLower = email.trim().toLowerCase();
        try {
            ResultSet rs = userDAO.getUserByEmailAndPassword(emailLower, password);
            if (rs != null && rs.next()) {
                User user = new User(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"), rs.getString("password"));
                if (onlineUserStreams.containsKey(emailLower)) {
                    menuView.sendFeedback("Account already logged in elsewhere.", AnsiColors.RED); return null;
                }
                menuView.sendFeedback("Login successful.", AnsiColors.GREEN); return user;
            } else {
                menuView.sendFeedback("Incorrect email or password.", AnsiColors.RED); return null;
            }
        } catch (SQLException e) {
            System.err.println(AnsiColors.RED + "[LOGIN DB ERROR] for " + emailLower + ": " + e.getMessage() + AnsiColors.RESET);
            menuView.sendFeedback("Server error during login.", AnsiColors.RED); return null;
        }

    }

    private User register(String name, String email, String password, String confirmPassword) throws IOException {

        try {
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                menuView.sendFeedback("All fields are required.", AnsiColors.RED); return null;
            }
            if (!ValidationUtils.isValidEmail(email)) {
                menuView.sendFeedback("Invalid email format.", AnsiColors.RED); return null;
            }
            if (!ValidationUtils.isValidPassword(password)) {
                menuView.sendFeedback("Password must be at least 8 chars, with uppercase, lowercase, and digit.", AnsiColors.RED); return null;
            }
            if (!password.equals(confirmPassword)) {
                menuView.sendFeedback("Passwords do not match.", AnsiColors.RED); return null;
            }
            if (userDAO.userExists(email)) {
                menuView.sendFeedback("Email already in use.", AnsiColors.RED); return null;
            }

            if (userDAO.insertUser(name, email, password, confirmPassword)) {
                ResultSet rs = userDAO.getUserByEmailAndPassword(email, password);
                if (rs != null && rs.next()) {
                    User user = new User(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"), rs.getString("password"));
                    menuView.sendFeedback("Registration successful!", AnsiColors.GREEN);
                    return user;
                } else {
                    System.err.println(AnsiColors.RED + "[REGISTER ERROR] Couldn't retrieve newly registered user: " + email + AnsiColors.RESET);
                    menuView.sendFeedback("Internal error after registration.", AnsiColors.RED); return null;
                }
            } else {
                menuView.sendFeedback("Registration failed (Database Error).", AnsiColors.RED); return null;
            }
        } catch (SQLException e) {
            System.err.println(AnsiColors.RED + "[REGISTER DB ERROR]: " + e.getMessage() + AnsiColors.RESET);
            menuView.sendFeedback("Database error during registration.", AnsiColors.RED); return null;
        } catch (Exception e) {
            System.err.println(AnsiColors.RED + "[REGISTER UNEXPECTED ERROR]: " + e.getMessage() + AnsiColors.RESET);
            menuView.sendFeedback("Unexpected error during registration.", AnsiColors.RED); return null;
        }

    }
}