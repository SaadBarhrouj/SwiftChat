package Server.controllers;

import Server.entities.Message;
import Server.entities.User;
import Server.entities.Group; // Importer Group
import Server.dao.ContactDAO;
import Server.dao.GroupDAO;
import Server.dao.MessageDAO;
import Server.dao.UserDAO;
import Server.utils.AnsiColors;
import Server.views.HelpView;
import Server.views.MenuView;

import java.io.*;
import java.net.SocketException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList; // Pour suppression
import java.util.List;
import java.util.Map;

public class ChatController {

    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final MessageDAO messageDAO;
    private final ContactDAO contactDAO;
    private final UserDAO userDAO;
    private final GroupDAO groupDAO;
    private final MenuView menuView;
    private final HelpView helpView;
    private final User userAccount;
    private final Map<String, DataOutputStream> onlineUserStreams;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String SERVER_STORAGE = "src/uploads/";

    public ChatController(DataInputStream dis, DataOutputStream dos, MessageDAO messageDAO, ContactDAO contactDAO, UserDAO userDAO, GroupDAO groupDAO, MenuView menuView, HelpView helpView, User userAccount, Map<String, DataOutputStream> onlineUserStreams) {
        this.dis = dis;
        this.dos = dos;
        this.messageDAO = messageDAO;
        this.contactDAO = contactDAO;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.menuView = menuView;
        this.helpView = helpView;
        this.userAccount = userAccount;
        this.onlineUserStreams = onlineUserStreams;
    }

    // --- Lanceurs de session ---

    public void handlePrivateChatSession() throws IOException {
        dos.writeUTF(AnsiColors.ANSI_CLS);
        dos.writeUTF("\nEntrez le surnom du contact (ou 'retour'):"); dos.flush();
        String nickname = dis.readUTF();
        if (nickname == null || nickname.trim().equalsIgnoreCase("retour") || nickname.trim().isEmpty()) { dos.writeUTF(AnsiColors.ANSI_CLS); return; }
        nickname = nickname.trim();
        int contactUserId = contactDAO.getUserIdByNickname(userAccount.getId(), nickname);
        if (contactUserId == -1) { menuView.sendFeedback("Contact '" + nickname + "' non trouvé.", AnsiColors.RED); menuView.promptContinue(); return; }
        String contactEmail = userDAO.getEmailById(contactUserId);
        if (contactEmail == null) { menuView.sendFeedback("Email du contact introuvable.", AnsiColors.RED); menuView.promptContinue(); return; }
        runChatLoop(nickname, contactUserId, contactEmail.toLowerCase(), -1);
    }

    public void handleGroupChatSession() throws IOException {
        dos.writeUTF(AnsiColors.ANSI_CLS);
        dos.writeUTF("\nNom du groupe (ou 'retour'):"); dos.flush();
        String groupName = dis.readUTF();
        if (groupName == null || groupName.trim().equalsIgnoreCase("retour") || groupName.trim().isEmpty()) { dos.writeUTF(AnsiColors.ANSI_CLS); return; }
        groupName = groupName.trim();
        int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) { menuView.sendFeedback("Groupe '" + groupName + "' non trouvé.", AnsiColors.RED); menuView.promptContinue(); return; }
        if (!groupDAO.getGroupMembers(groupId).contains(userAccount.getId())) { menuView.sendFeedback("Pas membre de '" + groupName + "'.", AnsiColors.RED); menuView.promptContinue(); return; }
        runChatLoop(groupName, -1, null, groupId);
    }


    // --- Boucle de Chat ---
    private void runChatLoop(String targetName, int contactUserId, String contactEmailLower, int groupId) throws IOException {
        boolean isGroupChat = (groupId > 0);
        boolean inChat = true;

        while (inChat && userAccount != null) {
            try {
                dos.writeUTF(AnsiColors.ANSI_CLS);
                String header = isGroupChat ? "=== Groupe: "+AnsiColors.MAGENTA+targetName+AnsiColors.RESET+" ===" : "=== Chat avec "+AnsiColors.YELLOW+targetName+AnsiColors.RESET+" ("+(onlineUserStreams.containsKey(contactEmailLower)?AnsiColors.GREEN+"enligne":AnsiColors.RED+"horsligne")+AnsiColors.RESET+") ===";
                dos.writeUTF(header);
                dos.writeUTF(AnsiColors.GRAY+"Cmds: [upload chemin], [view ID], [download ID], [delete ID], [help], [retour]"+AnsiColors.RESET);
                dos.writeUTF("--------------------------------------------------");

                List<Message> messages = isGroupChat ? messageDAO.getGroupMessages(groupId) : messageDAO.getConversation(userAccount.getId(), contactUserId);
                if (messages.isEmpty()) { dos.writeUTF("   (Aucun message)"); }
                else { /* Affichage formaté des messages */
                    for (Message msg : messages) {
                        String time = msg.getDate(); String sender;
                        if (msg.getSenderId() == userAccount.getId()) sender = AnsiColors.BLUE+"Vous"+AnsiColors.RESET; else sender = AnsiColors.YELLOW+(isGroupChat ? msg.getSenderEmail() : targetName)+AnsiColors.RESET;
                        String content; String idSuffix = "";
                        if ("deleted".equals(msg.getMessageType())) content = AnsiColors.GRAY+"[Supprimé]"+AnsiColors.RESET;
                        else { idSuffix = " "+AnsiColors.GRAY+"(ID:"+msg.getMessageId()+")"+AnsiColors.RESET; if ("file".equals(msg.getMessageType())) content = "[Fichier: "+AnsiColors.CYAN+msg.getFileName()+AnsiColors.RESET+"]"; else content = msg.getMessage(); }
                        dos.writeUTF(String.format(AnsiColors.GRAY+"(%s)"+AnsiColors.RESET+" %s: %s%s", time, sender, content, idSuffix));
                    }
                }

                dos.writeUTF("\n--------------------------------------------------");
                dos.writeUTF("> "); dos.flush();

                String input = dis.readUTF();
                if (input == null) throw new EOFException("Client disconnected.");
                input = input.trim(); if (input.isEmpty()) continue;

                if (input.equalsIgnoreCase("retour")) { inChat = false; dos.writeUTF(AnsiColors.ANSI_CLS); }
                else if (input.equalsIgnoreCase("help")) { if(isGroupChat) helpView.showGroupChatHelp(); else helpView.showChatHelp(); menuView.promptContinue(); }
                else if (input.toLowerCase().startsWith("upload ")) { dos.writeUTF("\n"+AnsiColors.BLUE+"Pour envoyer: retapez 'upload "+input.substring(7)+"' sur votre client.\n> "+AnsiColors.RESET); dos.flush();}
                else if (input.equals("CMD_INITIATE_UPLOAD")) { receiveFileFromClient(contactUserId, groupId); } // Appel local
                else if (input.toLowerCase().startsWith("view ")) { handleViewCommandWrapper(input); }
                else if (input.toLowerCase().startsWith("download ")) { handleDownloadCommandWrapper(input); }
                else if (input.toLowerCase().startsWith("delete ")) { handleDeleteMessageCommandWrapper(input); }
                else { // Message texte
                    int msgId = isGroupChat ? messageDAO.insertGroupMessage(userAccount.getId(), groupId, input) : messageDAO.insertMessage(userAccount.getId(), contactUserId, input);
                    if (msgId != -1) { if(isGroupChat) notifyGroupMembers(groupId, targetName, userAccount.getEmail(), msgId, userAccount.getId()); else notifyRecipientTextMessage(contactUserId, contactEmailLower, input, msgId); }
                    else { menuView.sendFeedback("Erreur envoi msg.", AnsiColors.RED); menuView.promptContinue(); }
                }

            } catch (EOFException | SocketException e) { inChat = false; throw e; }
            catch (IOException e) { menuView.sendFeedback("Erreur chat IO: "+e.getMessage(), AnsiColors.RED); menuView.promptContinue(); }
            catch (Exception e) { menuView.sendFeedback("Erreur chat: "+e.getMessage(), AnsiColors.RED); e.printStackTrace(); menuView.promptContinue(); }
        }
    }

    // --- Wrappers pour commandes ---
    private void handleViewCommandWrapper(String input) throws IOException {
        try { int id = Integer.parseInt(input.substring(5).trim()); handleViewCommand(id); }
        catch (Exception e){ menuView.sendFeedback("Usage: view <ID>", AnsiColors.RED); menuView.promptContinue(); }
    }
    private void handleDownloadCommandWrapper(String input) throws IOException {
        try { int id = Integer.parseInt(input.substring(9).trim()); handleDownloadCommand(id); }
        catch (Exception e){ menuView.sendFeedback("Usage: download <ID>", AnsiColors.RED); menuView.promptContinue(); }
    }
    private void handleDeleteMessageCommandWrapper(String input) throws IOException {
        try { int id = Integer.parseInt(input.substring(7).trim()); handleDeleteMessage(id); }
        catch (Exception e){ menuView.sendFeedback("Usage: delete <ID>", AnsiColors.RED); menuView.promptContinue(); }
    }

    // --- Logique des commandes de chat (View, Download, Delete, ReceiveFile) ---

    private void handleViewCommand(int messageId) throws IOException {
        // Logique de ClientHandler::handleViewCommand adaptée
        Message message = validateAndGetMessage(messageId); if (message == null) return;
        File file = getFileFromMessage(message); if (file == null) return;
        FileInputStream fis = null;
        try {
            dos.writeUTF("CMD_VIEW_FILE_START:" + file.getName()); dos.writeLong(file.length()); dos.flush();
            fis = new FileInputStream(file); byte[] buf = new byte[8192]; int read;
            while ((read = fis.read(buf)) != -1) dos.write(buf, 0, read);
            dos.flush(); dos.writeUTF("CMD_VIEW_FILE_END"); dos.flush();
        } catch (IOException e) { try { dos.writeUTF("CMD_VIEW_FILE_ERROR:"+e.getMessage()); dos.flush(); } catch (IOException ignored) {} throw e;} // Remonter erreur réseau
        finally { if (fis != null) try { fis.close(); } catch (IOException ignored) {} }
        // Pas de prompt ici, le client gère
    }

    private void handleDownloadCommand(int messageId) throws IOException {
        // Logique de ClientHandler::handleDownloadCommand adaptée
        dos.writeUTF("\nPréparation téléchargement ID: " + messageId); dos.flush();
        Message message = validateAndGetMessage(messageId); if (message == null) { menuView.promptContinue(); return; }
        File file = getFileFromMessage(message); if (file == null) { menuView.promptContinue(); return; }
        FileInputStream fis = null;
        try {
            // Réutiliser les commandes VIEW, le client fera la différence
            dos.writeUTF("CMD_VIEW_FILE_START:" + file.getName()); dos.writeLong(file.length()); dos.flush();
            fis = new FileInputStream(file); byte[] buf = new byte[8192]; int read;
            while ((read = fis.read(buf)) != -1) dos.write(buf, 0, read);
            dos.flush(); dos.writeUTF("CMD_VIEW_FILE_END"); dos.flush();
            menuView.sendFeedback("Transfert pour téléchargement terminé (ID: "+messageId+").", AnsiColors.GREEN);
        } catch (IOException e) { try { dos.writeUTF("CMD_DOWNLOAD_ERROR:"+e.getMessage()); dos.flush(); } catch (IOException ignored) {} menuView.promptContinue(); throw e; } // Remonter erreur réseau
        finally { if (fis != null) try { fis.close(); } catch (IOException ignored) {} }
        menuView.promptContinue(); // Pause après
    }

    private void handleDeleteMessage(int messageIdToDelete) throws IOException {
        // Logique de ClientHandler::handleDeleteMessageCommand adaptée
        if (userAccount == null) { menuView.sendFeedback("Non authentifié.", AnsiColors.RED); return; }
        boolean deleted = messageDAO.markMessageAsDeleted(messageIdToDelete, userAccount.getId());
        if (deleted) { dos.writeUTF(AnsiColors.GREEN + "\nMessage ID " + messageIdToDelete + " supprimé." + AnsiColors.RESET); dos.flush(); notifyOthersOfDeletion(messageIdToDelete); }
        else { menuView.sendFeedback("Impossible de supprimer ID " + messageIdToDelete + ".", AnsiColors.RED); menuView.promptContinue(); }
        // Pas de prompt si succès
    }

    private void receiveFileFromClient(int recipientUserId, int groupId) throws IOException {
        // Logique de ClientHandler::receiveFileFromClient adaptée
        String originalFileName = null; long fileSize = -1; int messageId = -1;
        File serverFile = null; FileOutputStream fos = null; long bytesReceived = 0;
        boolean isGroup = (groupId > 0); String context = isGroup ? "groupe " + groupId : "privé " + recipientUserId;
        try {
            originalFileName = dis.readUTF(); fileSize = dis.readLong();
            if (originalFileName == null || originalFileName.trim().isEmpty() || fileSize < 0) throw new IOException("Invalid file metadata.");
            originalFileName = originalFileName.replaceAll("[^a-zA-Z0-9.\\-_ ]", "_").trim(); if (originalFileName.isEmpty()) originalFileName = "file";
            System.out.println("[UPLOAD] Receiving '" + originalFileName + "' from " + userAccount.getEmail() + " for " + context);
            messageId = isGroup ? messageDAO.insertGroupFileMessage(userAccount.getId(), groupId, originalFileName) : messageDAO.insertFileMessage(userAccount.getId(), recipientUserId, originalFileName);
            if (messageId == -1) { dos.writeUTF("CMD_UPLOAD_ERROR:Server DB Error"); dos.flush(); try { if (fileSize > 0) dis.skipBytes(Math.min((int)fileSize, 1024*1024)); } catch (Exception ignored) {} return; }
            String serverFileName = messageId + "_" + originalFileName; serverFile = new File(SERVER_STORAGE + serverFileName);
            File pDir = serverFile.getParentFile(); if (pDir != null && !pDir.exists() && !pDir.mkdirs()) throw new IOException("Cannot create storage dir.");
            dos.writeUTF("CMD_UPLOAD_READY:" + messageId); dos.flush(); System.out.println("[UPLOAD] Ready for ID " + messageId);
            fos = new FileOutputStream(serverFile); byte[] buf = new byte[8192]; int read;
            while (bytesReceived < fileSize && (read = dis.read(buf, 0, (int) Math.min(buf.length, fileSize - bytesReceived))) != -1) { fos.write(buf, 0, read); bytesReceived += read; }
            fos.flush();
            if (bytesReceived == fileSize) { dos.writeUTF("CMD_UPLOAD_SUCCESS"); dos.flush(); System.out.println("[UPLOAD SUCCESS] ID " + messageId); if (isGroup) notifyGroupMembers(groupId, getGroupNameById(groupId), userAccount.getEmail(), messageId, userAccount.getId()); else notifyRecipientFileShared(recipientUserId, userDAO.getEmailById(recipientUserId), originalFileName, messageId); }
            else { throw new IOException("Incomplete upload: " + bytesReceived + "/" + fileSize); }
        } catch (IOException e) { System.err.println("[UPLOAD FAILED] " + e.getMessage()); try { dos.writeUTF("CMD_UPLOAD_ERROR:" + e.getMessage()); dos.flush(); } catch (IOException ignored) {} if (fos != null) try { fos.close(); } catch (IOException ignored) {} fos = null; if (serverFile != null && serverFile.exists() && !serverFile.delete()) System.err.println("Failed cleanup partial file " + serverFile.getName()); if(messageId != -1) System.err.println("Msg ID "+messageId+" may exist without file."); }
        finally { if (fos != null) try { fos.close(); } catch (IOException ignored) {} }
    }


    // --- Méthodes de Notification (dupliquées de GroupController ou service partagé) ---
    // Il serait MIEUX d'avoir un NotificationService injecté ici et dans GroupController
    private void notifyRecipientTextMessage(int recipientUserId, String recipientEmail, String messageText, int messageId) {
        if (recipientEmail == null) return; String recipientEmailLower = recipientEmail.toLowerCase();
        if (onlineUserStreams.containsKey(recipientEmailLower)) { try { DataOutputStream rDos = onlineUserStreams.get(recipientEmailLower); String t = TIME_FORMATTER.format(java.time.LocalTime.now()); rDos.writeUTF(String.format("\n[%s] %s: %s\n> ", t, userAccount.getEmail(), messageText)); rDos.flush(); } catch (IOException e) { messageDAO.storePendingMessage(recipientUserId, messageId); onlineUserStreams.remove(recipientEmailLower); userDAO.setUserOnlineStatus(recipientUserId, false);} }
        else { messageDAO.storePendingMessage(recipientUserId, messageId); }
    }
    private void notifyGroupMembers(int groupId, String groupName, String senderEmail, int messageId, int senderId) {
        Message message = messageDAO.getMessageById(messageId); if (message == null) return; List<Integer> memberIds = groupDAO.getGroupMembers(groupId); String notificationText; String t = TIME_FORMATTER.format(java.time.LocalTime.now());
        if ("file".equals(message.getMessageType())) { notificationText = String.format(AnsiColors.CYAN+"[Fichier] %s: %s (ID: %d)"+AnsiColors.RESET, senderEmail, message.getFileName(), messageId); } else { notificationText = senderEmail + ": " + message.getMessage(); }
        for (int memberId : memberIds) { if (memberId == senderId) continue; String rEmail = userDAO.getEmailById(memberId); if (rEmail != null) { String rEmailLower = rEmail.toLowerCase(); if (onlineUserStreams.containsKey(rEmailLower)) { try { DataOutputStream rDos = onlineUserStreams.get(rEmailLower); rDos.writeUTF(String.format("\n[%s] [%s] %s\n> ", t, groupName, notificationText)); rDos.flush(); } catch (IOException e) { if (!"deleted".equals(message.getMessageType())) messageDAO.storePendingMessage(memberId, messageId); onlineUserStreams.remove(rEmailLower); userDAO.setUserOnlineStatus(memberId, false); } } else { if (!"deleted".equals(message.getMessageType())) messageDAO.storePendingMessage(memberId, messageId); } } }
    }
    private void notifyOthersOfDeletion(int deletedMessageId) {
        Message deletedMsgInfo = messageDAO.getMessageById(deletedMessageId); if (deletedMsgInfo == null) return; List<Integer> targetUserIds = new ArrayList<>(); int groupId = deletedMsgInfo.getGroupId();
        if (groupId > 0) { List<Integer> members = groupDAO.getGroupMembers(groupId); for(int mId : members) if(mId != userAccount.getId()) targetUserIds.add(mId); }
        else { int rId = userDAO.getUserIdByEmail(deletedMsgInfo.getReceiverEmail()); if(rId > 0 && rId != userAccount.getId()) targetUserIds.add(rId); }
        String cmd = "CMD_MSG_DELETED:" + deletedMessageId;
        for (int targetId : targetUserIds) { String targetEmail = userDAO.getEmailById(targetId); if (targetEmail != null) { String targetEmailLower = targetEmail.toLowerCase(); if (onlineUserStreams.containsKey(targetEmailLower)) { try { onlineUserStreams.get(targetEmailLower).writeUTF("\n" + cmd + "\n> "); onlineUserStreams.get(targetEmailLower).flush(); } catch (IOException e) { onlineUserStreams.remove(targetEmailLower); userDAO.setUserOnlineStatus(targetId, false); } } } }
    }
    private void notifyRecipientFileShared(int recipientUserId, String recipientNicknameOrEmail, String fileName, int messageId) {
        String rEmail = userDAO.getEmailById(recipientUserId); if (rEmail == null) return; String rEmailLower = rEmail.toLowerCase();
        if (onlineUserStreams.containsKey(rEmailLower)) { try { DataOutputStream rDos = onlineUserStreams.get(rEmailLower); String t = TIME_FORMATTER.format(java.time.LocalTime.now()); String notif = String.format("\n[%s] [Fichier] %s: %s (ID: %d)\n -> Use view/download %d\n> ", t, userAccount.getEmail(), fileName, messageId, messageId); rDos.writeUTF(notif); rDos.flush(); } catch (IOException e) { messageDAO.storePendingMessage(recipientUserId, messageId); onlineUserStreams.remove(rEmailLower); userDAO.setUserOnlineStatus(recipientUserId, false);} }
        else { messageDAO.storePendingMessage(recipientUserId, messageId); }
    }

    // --- Méthodes utilitaires ---
    private Message validateAndGetMessage(int messageId) throws IOException {
        // Logique de ClientHandler::validateAndGetMessage adaptée
        Message message = messageDAO.getMessageById(messageId);
        if (message == null) { menuView.sendFeedback("ID " + messageId + " introuvable.", AnsiColors.RED); return null; }
        if (!"file".equals(message.getMessageType()) && !"deleted".equals(message.getMessageType())) { menuView.sendFeedback("ID " + messageId + " n'est pas un fichier.", AnsiColors.RED); return null; }
        if ("deleted".equals(message.getMessageType())) { menuView.sendFeedback("ID " + messageId + " déjà supprimé.", AnsiColors.YELLOW); return null; }
        boolean ok = false; int gid = message.getGroupId();
        if (gid <= 0) { ok = (message.getSenderId() == userAccount.getId()) || (userDAO.getUserIdByEmail(message.getReceiverEmail()) == userAccount.getId()); }
        else { ok = groupDAO.getGroupMembers(gid).contains(userAccount.getId()); }
        if (!ok) { menuView.sendFeedback("Permission refusée pour ID " + messageId, AnsiColors.RED); return null; }
        return message;
    }
    private File getFileFromMessage(Message message) throws IOException {
        // Logique de ClientHandler::getFileFromMessage adaptée
        if (message == null || !"file".equals(message.getMessageType()) || message.getFileName() == null) return null;
        String safeName = message.getFileName().replaceAll("[^a-zA-Z0-9.\\-_ ]", "_").trim(); if(safeName.isEmpty()) safeName="file";
        String path = SERVER_STORAGE + message.getMessageId() + "_" + safeName; File file = new File(path);
        if (!file.exists()) { menuView.sendFeedback("Fichier serveur manquant pour ID " + message.getMessageId(), AnsiColors.RED); return null; }
        if (!file.isFile()) { menuView.sendFeedback("Chemin serveur invalide pour ID " + message.getMessageId(), AnsiColors.RED); return null; }
        if (!file.canRead()) { menuView.sendFeedback("Permission serveur refusée pour ID " + message.getMessageId(), AnsiColors.RED); return null; }
        return file;
    }
    private String getGroupNameById(int groupId) {
        // Logique de ClientHandler::getGroupNameById adaptée
        List<Group> userGroups = groupDAO.getGroupsForUser(userAccount.getId());
        for(Group g : userGroups) { if(g.getId() == groupId) return g.getName(); }
        return "Groupe ID " + groupId;
    }

} // Fin ChatController