package Server;

import Server.controllers.*;
import Server.dao.*;
import Server.entities.*;
import Server.utils.*;
import Server.views.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler extends Thread {
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final Socket commthread;
    private volatile boolean Auth;
    private User userAccount;
    private Connection conn;

    private UserDAO userDAO;
    private ContactDAO contactDAO;
    private MessageDAO messageDAO;
    private GroupDAO groupDAO;

    private static final Map<String, DataOutputStream> onlineUserStreams = new ConcurrentHashMap<>();

    private MenuView menuView;
    private HelpView helpView;

    private AuthenticationController authController;
    private ContactController contactController;
    private GroupController groupController;
    private ChatController chatController;
    private ProfileController profileController;

    public ClientHandler(Socket s, DataInputStream diss, DataOutputStream doss) {
        this.commthread = s;
        this.dis = diss;
        this.dos = doss;
        this.Auth = false;
        System.out.println("Initializing ClientHandler for " + s.getRemoteSocketAddress() + "...");

        try {
            this.conn = DatabaseConnection.getConnection();
            if (this.conn == null) throw new SQLException("DB connection failed.");
            System.out.println("--> DB connection obtained.");

            this.userDAO = new UserDAO(this.conn);
            this.contactDAO = new ContactDAO(this.conn);
            this.messageDAO = new MessageDAO(this.conn);
            this.groupDAO = new GroupDAO(this.conn);
            System.out.println("--> DAOs initialized.");

            this.menuView = new MenuView(this.dis, this.dos);
            this.helpView = new HelpView(this.dos);
            System.out.println("--> Views initialized.");

            this.authController = new AuthenticationController(dis, dos, userDAO, menuView, helpView, onlineUserStreams);
            System.out.println("--> AuthController initialized.");

            File storageDir = new File(AppPaths.SERVER_UPLOADS_DIR);
            if (!storageDir.exists() && !storageDir.mkdirs()) { throw new IOException("Failed to create storage directory."); }
            if (!storageDir.isDirectory() || !storageDir.canWrite()) { throw new IOException("Storage directory invalid/unwritable."); }
            System.out.println("--> Storage directory verified.");

            System.out.println("ClientHandler initialized successfully.");

        } catch (Exception e) {
            System.err.println(AnsiColors.RED + "FATAL ERROR during ClientHandler initialization: " + e.getMessage() + AnsiColors.RESET);
            e.printStackTrace();
            cleanup();
        }
    }

    @Override
    public void run() {
        String clientIdentifier = getClientIdentifier();
        try {
            this.userAccount = authController.handleAuthentication();

            if (this.userAccount != null) {
                this.Auth = true;
                clientIdentifier = getClientIdentifier();
                initializeControllersPostAuth();
                postAuthenticationTasks();
                System.out.println(AnsiColors.GREEN + "[CONNECTION] " + clientIdentifier + " ready." + AnsiColors.RESET);

                while (this.Auth) {
                    try {
                        menuView.showUserMenu();
                        String choice = dis.readUTF();
                        if (choice == null) throw new EOFException("Client disconnected.");
                        String trimmedChoice = choice.trim();

                        switch (trimmedChoice) {
                            case "1": contactController.handleMenu(); break;
                            case "2": chatController.handlePrivateChatSession(); break;
                            case "3": groupController.handleMenu(); break;
                            case "4": profileController.updateProfile(); break;
                            case "5": logout(); break;
                            default: menuView.sendFeedback("Invalid choice (1-5).", AnsiColors.RED); menuView.promptContinue(); break;
                        }
                    } catch (EOFException | SocketException e) {
                        System.err.println(AnsiColors.YELLOW + "[DISCONNECT] " + clientIdentifier + " disconnected: " + e.getMessage() + AnsiColors.RESET);
                        this.Auth = false; errorCleanup();
                    } catch (IOException e) {
                        System.err.println(AnsiColors.RED + "[IO ERROR] Loop (" + clientIdentifier + "): " + e.getMessage() + AnsiColors.RESET);
                        e.printStackTrace(); this.Auth = false; errorCleanup();
                    } catch (Exception e) {
                        System.err.println(AnsiColors.RED + "[UNEXPECTED ERROR] Loop (" + clientIdentifier + "): " + e.getMessage() + AnsiColors.RESET);
                        e.printStackTrace(); this.Auth = false; errorCleanup();
                    }
                }
                System.out.println(AnsiColors.BLUE + "[DISCONNECT] " + clientIdentifier + " logged out normally." + AnsiColors.RESET);

            } else {
                System.out.println(AnsiColors.YELLOW + "[AUTH FAILED/INTERRUPTED] for " + clientIdentifier + "." + AnsiColors.RESET);
            }

        } catch (IOException e) {
            System.err.println(AnsiColors.RED + "[FATAL IO ERROR] (" + clientIdentifier + "): " + e.getMessage() + AnsiColors.RESET);
            if(!(e instanceof EOFException || e instanceof SocketException)) e.printStackTrace();
            errorCleanup();
        } catch (Exception e) {
            System.err.println(AnsiColors.RED + "[FATAL UNEXPECTED ERROR] (" + clientIdentifier + "): " + e.getMessage() + AnsiColors.RESET);
            e.printStackTrace();
            errorCleanup();
        } finally {
            cleanup();
            System.out.println(AnsiColors.GRAY + "ClientHandler thread finished for " + clientIdentifier + "." + AnsiColors.RESET);
        }
    }

    private void initializeControllersPostAuth() {
        if (userAccount == null) { System.err.println("[INIT CTRL ERROR] No userAccount."); return; }
        this.contactController = new ContactController(dis, dos, contactDAO, userDAO, menuView, userAccount, onlineUserStreams);
        this.chatController = new ChatController(dis, dos, messageDAO, contactDAO, userDAO, groupDAO, menuView, helpView, userAccount, onlineUserStreams);
        this.groupController = new GroupController(dis, dos, groupDAO, userDAO, messageDAO, menuView, helpView, userAccount, onlineUserStreams);
        this.profileController = new ProfileController(dis, dos, userDAO, menuView, userAccount, onlineUserStreams);
        this.groupController.setChatController(this.chatController);
        System.out.println("--> Post-auth controllers initialized for " + userAccount.getEmail());
    }

    private void postAuthenticationTasks() {
        if (userAccount == null) return;
        String emailLower = userAccount.getEmail().toLowerCase();
        try {
            onlineUserStreams.put(emailLower, dos);
            userDAO.setUserOnlineStatus(userAccount.getId(), true);
            System.out.println(AnsiColors.GRAY+"[POST-AUTH] "+userAccount.getEmail()+" map/DB updated."+AnsiColors.RESET);
            boolean hasNew = receiveAndDeletePendingNotifications();
             if (!hasNew) menuView.sendFeedback("No new offline messages.", AnsiColors.GREEN);
             menuView.sendFeedback("Welcome " + userAccount.getName() + "!", AnsiColors.GREEN);
        } catch (Exception e) {
            System.err.println(AnsiColors.RED+"[POST-AUTH ERROR] for "+userAccount.getEmail()+": "+e.getMessage()+AnsiColors.RESET);
            try { menuView.sendFeedback("Erreur serveur post-connexion.", AnsiColors.RED); } catch (IOException ignored) {}
            this.Auth = false; errorCleanup();
        }
    }

    private void logout() {
        String userEmail = getClientIdentifier();
        System.out.println(AnsiColors.BLUE + "[LOGOUT] " + userEmail + " initiated." + AnsiColors.RESET);
        try { dos.writeUTF(AnsiColors.ANSI_CLS + AnsiColors.GREEN + "Deconnexion..." + AnsiColors.RESET); dos.flush(); } catch (IOException e) { /* Ignore */ }
        this.Auth = false;
        errorCleanup();
    }

    private void errorCleanup() {
        String clientDesc = getClientIdentifier();
        System.out.println(AnsiColors.YELLOW + "Error/logout cleanup for " + clientDesc + "..." + AnsiColors.RESET);
        this.Auth = false;
        if (userAccount != null) {
            String emailLower = userAccount.getEmail().toLowerCase();
            if (onlineUserStreams.remove(emailLower) != null) System.out.println(AnsiColors.GRAY+"[CLEANUP] Removed "+userAccount.getEmail()+" from map."+AnsiColors.RESET);
            try { userDAO.setUserOnlineStatus(userAccount.getId(), false); System.out.println(AnsiColors.GRAY+"[CLEANUP] Set "+userAccount.getEmail()+" offline in DB."+AnsiColors.RESET); }
            catch (Exception e) { System.err.println(AnsiColors.RED+"[CLEANUP DB ERR] "+e.getMessage()+AnsiColors.RESET); }
            notifyContactsOfLogout();
        }
    }

    private void cleanup() {
        String clientDesc = getClientIdentifier();
        System.out.println(AnsiColors.GRAY + "Final cleanup for " + clientDesc + "..." + AnsiColors.RESET);
        try { if (dis != null) dis.close(); } catch (IOException e) { /* ignore */ }
        try { if (dos != null) dos.close(); } catch (IOException e) { /* ignore */ }
        try { if (commthread != null && !commthread.isClosed()) commthread.close(); } catch (IOException e) { /* ignore */ }
        try { if (this.conn != null && !this.conn.isClosed()) { this.conn.close(); System.out.println(AnsiColors.GRAY + "[CLEANUP] DB connection closed." + AnsiColors.RESET); } }
        catch (SQLException e) { System.err.println(AnsiColors.RED + "[CLEANUP DB CLOSE ERROR]: " + e.getMessage() + AnsiColors.RESET); }
        System.out.println(AnsiColors.GRAY + "Cleanup finished." + AnsiColors.RESET);
    }

    private String getClientIdentifier() {
        if (userAccount != null && userAccount.getEmail() != null) return userAccount.getEmail() + " (" + (commthread!=null?commthread.getRemoteSocketAddress():"no socket") + ")";
        return (commthread != null ? commthread.getRemoteSocketAddress().toString() : "unknown");
    }

    private boolean receiveAndDeletePendingNotifications() throws IOException {
        if (userAccount == null) return false;
        boolean hasNew = false;
        List<Message> pendingItems = messageDAO.getPendingMessagesForUser(userAccount.getId());
        if (!pendingItems.isEmpty()) {
            hasNew = true;
            StringBuilder sb = new StringBuilder("\n--- Notifications offLine ---\n");
            for (Message item : pendingItems) {
                String time = item.getDate(); String sender = (item.getSenderEmail()!=null)?item.getSenderEmail():"ID "+item.getSenderId(); String ctx = (item.getGroupId()>0)?" (Groupe: "+getGroupNameById(item.getGroupId())+")":"";
                if ("file".equals(item.getMessageType())) sb.append(String.format("[%s] Fichier de %s%s : '%s' (ID: %d) -> Use view/download\n", time, sender, ctx, item.getFileName(), item.getMessageId()));
                else sb.append(String.format("[%s] Msg de %s%s : %s\n", time, sender, ctx, item.getMessage()));
            }
            sb.append("----------------------------\n"); dos.writeUTF(sb.toString()); dos.flush();
            try { messageDAO.deletePendingMessagesForUser(userAccount.getId()); }
            catch(Exception e) { System.err.println("Error deleting pending for "+userAccount.getId()+": "+e.getMessage());}
        }
        return hasNew;
    }

    private void notifyContactsOfLogout() {
        if (userAccount == null) return;
        List<Contact> contacts = contactDAO.getContacts(userAccount.getId());
        for (Contact c : contacts) {
            String email = userDAO.getEmailById(c.getContactUserId());
            if (email != null) {
                String emailLower = email.toLowerCase();
                if (onlineUserStreams.containsKey(emailLower)) {
                    try { onlineUserStreams.get(emailLower).writeUTF("\n[STATUS] " + userAccount.getEmail() + " est hors ligne.\n> "); onlineUserStreams.get(emailLower).flush(); }
                    catch (IOException e) { onlineUserStreams.remove(emailLower); try{userDAO.setUserOnlineStatus(c.getContactUserId(), false);}catch(Exception dbE){}}
                }
            }
        }
    }

    private String getGroupNameById(int groupId) {
        List<Group> userGroups = groupDAO.getGroupsForUser(userAccount.getId());
        for(Group g : userGroups) { if(g.getId() == groupId) return g.getName(); }
        return "Group ID " + groupId;
    }

}