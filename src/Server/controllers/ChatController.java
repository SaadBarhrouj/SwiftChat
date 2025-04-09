package Server.controllers;

import Server.dao.*;
import Server.entities.*;
import Server.utils.*;
import Server.views.*;
import java.io.*;
import java.net.SocketException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    public void handlePrivateChatSession() throws IOException {
        dos.writeUTF(AnsiColors.ANSI_CLS);
        dos.writeUTF("\nEnter contact nickname (or 'back'):"); dos.flush();
        String nickname = dis.readUTF();
        if (nickname == null || nickname.trim().equalsIgnoreCase("back") || nickname.trim().isEmpty()) { dos.writeUTF(AnsiColors.ANSI_CLS); return; }
        nickname = nickname.trim();
        int contactUserId = contactDAO.getUserIdByNickname(userAccount.getId(), nickname);
        if (contactUserId == -1) { menuView.sendFeedback("Contact '" + nickname + "' not found.", AnsiColors.RED); menuView.promptContinue(); return; }
        String contactEmail = userDAO.getEmailById(contactUserId);
        if (contactEmail == null) { menuView.sendFeedback("Contact email missing.", AnsiColors.RED); menuView.promptContinue(); return; }
        runChatLoop(nickname, contactUserId, contactEmail.toLowerCase(), -1);
    }

    public void handleGroupChatSession() throws IOException {
        dos.writeUTF(AnsiColors.ANSI_CLS);
        dos.writeUTF("\nEnter group name (or 'back'):"); dos.flush();
        String groupName = dis.readUTF();
        if (groupName == null || groupName.trim().equalsIgnoreCase("back") || groupName.trim().isEmpty()) { dos.writeUTF(AnsiColors.ANSI_CLS); return; }
        groupName = groupName.trim();
        int groupId = groupDAO.getGroupIdByName(groupName);
        if (groupId == -1) { menuView.sendFeedback("Group '" + groupName + "' not found.", AnsiColors.RED); menuView.promptContinue(); return; }
        if (!groupDAO.getGroupMembers(groupId).contains(userAccount.getId())) { menuView.sendFeedback("Not a member of '" + groupName + "'.", AnsiColors.RED); menuView.promptContinue(); return; }
        runChatLoop(groupName, -1, null, groupId);
    }

    private void runChatLoop(String targetName, int contactUserId, String contactEmailLower, int groupId) throws IOException {
        boolean isGroupChat = (groupId > 0);
        boolean inChat = true;
        while (inChat && userAccount != null) {
            try {
                dos.writeUTF(AnsiColors.ANSI_CLS);
                String header = isGroupChat ? "=== Group: "+AnsiColors.MAGENTA+targetName+AnsiColors.RESET+" ===" : "=== Chat with "+AnsiColors.YELLOW+targetName+AnsiColors.RESET+" ("+(onlineUserStreams.containsKey(contactEmailLower)?AnsiColors.GREEN+"[Online]":AnsiColors.RED+"[Offline]")+AnsiColors.RESET+") ===";
                dos.writeUTF(header);
                dos.writeUTF(AnsiColors.GRAY+"Cmds: [upload path/file], [view ID], [download ID], [delete ID], [help], [back]"+AnsiColors.RESET);
                dos.writeUTF("--------------------------------------------------");
                List<Message> messages = isGroupChat ? messageDAO.getGroupMessages(groupId) : messageDAO.getConversation(userAccount.getId(), contactUserId);
                if (messages.isEmpty()) { dos.writeUTF("   (No messages yet)"); }
                else { for (Message msg : messages) { String t=msg.getDate(); String s=(msg.getSenderId()==userAccount.getId())?AnsiColors.BLUE+"You":AnsiColors.YELLOW+(isGroupChat?msg.getSenderEmail():targetName); String c; String idS=""; if("deleted".equals(msg.getMessageType())) c=AnsiColors.GRAY+"[Deleted]"; else { idS=" "+AnsiColors.GRAY+"(ID:"+msg.getMessageId()+")"; if("file".equals(msg.getMessageType())) c="[File: "+AnsiColors.CYAN+msg.getFileName()+AnsiColors.RESET+"]"; else c=msg.getMessage(); } dos.writeUTF(String.format(AnsiColors.GRAY+"(%s)"+AnsiColors.RESET+" %s: %s%s", t, s+AnsiColors.RESET, c, idS)); } }
                dos.writeUTF("\n--------------------------------------------------");
                dos.writeUTF("> "); dos.flush();
                String input = dis.readUTF();
                if (input == null) throw new EOFException("Client disconnected.");
                input = input.trim(); if (input.isEmpty()) continue;

                if (input.equalsIgnoreCase("back")) { inChat = false; dos.writeUTF(AnsiColors.ANSI_CLS); }
                else if (input.equalsIgnoreCase("help")) { if(isGroupChat) helpView.showGroupChatHelp(); else helpView.showChatHelp(); menuView.promptContinue(); }
                else if (input.toLowerCase().startsWith("upload ")) { dos.writeUTF("\n"+AnsiColors.BLUE+"To send: Re-type 'upload "+input.substring(7)+"' on YOUR client.\n> "+AnsiColors.RESET); dos.flush();}
                else if (input.equals("CMD_INITIATE_UPLOAD")) { System.out.println("[SERVER] Detected CMD_INITIATE_UPLOAD in chat loop."); receiveFileFromClientNew(contactUserId, groupId); } // *** Appel Nouvelle Méthode ***
                else if (input.toLowerCase().startsWith("view ")) { handleViewCommandWrapper(input); }
                else if (input.toLowerCase().startsWith("download ")) { handleDownloadCommandWrapper(input); }
                else if (input.toLowerCase().startsWith("delete ")) { handleDeleteMessageCommandWrapper(input); }
                else { int msgId = isGroupChat ? messageDAO.insertGroupMessage(userAccount.getId(), groupId, input) : messageDAO.insertMessage(userAccount.getId(), contactUserId, input); if (msgId!=-1) { if(isGroupChat) notifyGroupMembers(groupId, targetName, userAccount.getEmail(), msgId, userAccount.getId()); else notifyRecipientTextMessage(contactUserId, contactEmailLower, input, msgId); } else { menuView.sendFeedback("Error sending msg.", AnsiColors.RED); menuView.promptContinue(); } }
            } catch (EOFException | SocketException e) { inChat = false; throw e; }
            catch (IOException e) { menuView.sendFeedback("Chat IO Error: "+e.getMessage(), AnsiColors.RED); menuView.promptContinue(); }
            catch (Exception e) { menuView.sendFeedback("Chat Error: "+e.getMessage(), AnsiColors.RED); e.printStackTrace(); menuView.promptContinue(); }
        }
    }


    private void handleViewCommandWrapper(String input) throws IOException { try { int id = Integer.parseInt(input.substring(5).trim()); handleViewCommand(id); } catch (Exception e){ menuView.sendFeedback("Usage: view <ID>", AnsiColors.RED); menuView.promptContinue(); } }
    private void handleDownloadCommandWrapper(String input) throws IOException { try { int id = Integer.parseInt(input.substring(9).trim()); handleDownloadCommand(id); } catch (Exception e){ menuView.sendFeedback("Usage: download <ID>", AnsiColors.RED); menuView.promptContinue(); } }
    private void handleDeleteMessageCommandWrapper(String input) throws IOException { try { int id = Integer.parseInt(input.substring(7).trim()); handleDeleteMessage(id); } catch (Exception e){ menuView.sendFeedback("Usage: delete <ID>", AnsiColors.RED); menuView.promptContinue(); } }


    private void handleViewCommand(int messageId) throws IOException { Message m=validateAndGetMessageForFileOps(messageId); if(m==null)return; File f=getFileFromMessage(m); if(f==null)return; FileInputStream fis=null; try {dos.writeUTF("CMD_VIEW_FILE_START:"+f.getName()); dos.writeLong(f.length()); dos.flush(); fis=new FileInputStream(f); byte[]b=new byte[8192];int r; while((r=fis.read(b))!=-1)dos.write(b,0,r); dos.flush(); dos.writeUTF("CMD_VIEW_FILE_END"); dos.flush();} catch(IOException e){try{dos.writeUTF("CMD_VIEW_FILE_ERROR:"+e.getMessage()); dos.flush();}catch(IOException i){}throw e;}finally{if(fis!=null)try{fis.close();}catch(IOException i){}} }
    private void handleDownloadCommand(int messageId) throws IOException { dos.writeUTF("\nPrep download ID: "+messageId);dos.flush(); Message m=validateAndGetMessageForFileOps(messageId); if(m==null){menuView.promptContinue();return;} File f=getFileFromMessage(m); if(f==null){menuView.promptContinue();return;} FileInputStream fis=null; try {dos.writeUTF("CMD_VIEW_FILE_START:"+f.getName()); dos.writeLong(f.length()); dos.flush(); fis=new FileInputStream(f); byte[]b=new byte[8192];int r; while((r=fis.read(b))!=-1)dos.write(b,0,r); dos.flush(); dos.writeUTF("CMD_VIEW_FILE_END"); dos.flush(); menuView.sendFeedback("Download transfer complete (ID: "+messageId+").", AnsiColors.GREEN);} catch(IOException e){try{dos.writeUTF("CMD_DOWNLOAD_ERROR:"+e.getMessage()); dos.flush();}catch(IOException i){} menuView.promptContinue(); throw e;}finally{if(fis!=null)try{fis.close();}catch(IOException i){}} menuView.promptContinue(); }
    private void handleDeleteMessage(int messageIdToDelete) throws IOException { if(userAccount==null){menuView.sendFeedback("Not auth.",AnsiColors.RED);return;} boolean d=messageDAO.markMessageAsDeleted(messageIdToDelete,userAccount.getId()); if(d){dos.writeUTF(AnsiColors.GREEN+"\nMsg ID "+messageIdToDelete+" deleted."+AnsiColors.RESET);dos.flush();notifyOthersOfDeletion(messageIdToDelete);}else{menuView.sendFeedback("Cannot delete ID "+messageIdToDelete+".",AnsiColors.RED);menuView.promptContinue();} }


    private void receiveFileFromClientNew(int recipientUserId, int groupId) throws IOException {
        System.out.println("[SERVER UPLOAD NEW] >> Entered receiveFileFromClientNew");
        String originalFileName = null; long fileSize = -1; int messageId = -1;
        File serverFile = null; FileOutputStream fos = null; long bytesReceived = 0;
        boolean isGroup = (groupId > 0); String context = isGroup ? "group " + groupId : "private " + recipientUserId;
        boolean success = false;

        try {
            System.out.println("[SERVER UPLOAD NEW] Waiting for metadata...");
            originalFileName = dis.readUTF(); fileSize = dis.readLong();
            System.out.println("[SERVER UPLOAD NEW] Received Meta: Name="+originalFileName+", Size="+fileSize);
            if (originalFileName == null || originalFileName.trim().isEmpty() || fileSize < 0) throw new IOException("Invalid file metadata.");
            originalFileName = originalFileName.replaceAll("[^a-zA-Z0-9.\\-_ ]", "_").trim(); if (originalFileName.isEmpty()) originalFileName = "file";

            System.out.println("[SERVER UPLOAD NEW] Inserting DB record for " + context);
            messageId = isGroup ? messageDAO.insertGroupFileMessage(userAccount.getId(), groupId, originalFileName) : messageDAO.insertFileMessage(userAccount.getId(), recipientUserId, originalFileName);
            System.out.println("[SERVER UPLOAD NEW] DB Msg ID: " + messageId);
            if (messageId == -1) { throw new IOException("Server DB Error: Could not create file message record."); }

            String serverFileName = messageId + "_" + originalFileName; serverFile = new File(AppPaths.SERVER_UPLOADS_DIR + serverFileName);
            File pDir = serverFile.getParentFile(); if (pDir != null && !pDir.exists() && !pDir.mkdirs()) throw new IOException("Cannot create storage dir: "+pDir.getAbsolutePath());



            System.out.println("[SERVER UPLOAD NEW] Receiving data to " + serverFile.getAbsolutePath());
            fos = new FileOutputStream(serverFile); byte[] buf = new byte[8192]; int read;
            while (bytesReceived < fileSize && (read = dis.read(buf, 0, (int) Math.min(buf.length, fileSize - bytesReceived))) != -1) {
                fos.write(buf, 0, read); bytesReceived += read;
                System.out.print("\r[SERVER UPLOAD NEW ID:"+messageId+"] Received: "+bytesReceived+"/"+fileSize);
            }
            System.out.println(); fos.flush(); fos.close(); fos = null;

            if (bytesReceived == fileSize) {
                System.out.println("[SERVER UPLOAD NEW SUCCESS] File ID " + messageId + " fully received.");
                success = true;

            } else { throw new IOException("Incomplete upload. Received " + bytesReceived + "/" + fileSize + " bytes."); }

        } catch (IOException e) {
            System.err.println(AnsiColors.RED + "[SERVER UPLOAD NEW FAILED] ID:" + messageId + " Err: " + e.getMessage() + AnsiColors.RESET);
            success = false;
            if (fos != null) { try { fos.close(); } catch (IOException ex) {} fos = null; }
            if (serverFile != null && serverFile.exists() && !serverFile.delete()) { System.err.println("[SERVER UPLOAD CLEANUP] Failed delete: " + serverFile.getAbsolutePath()); }
            if(messageId != -1) { System.err.println("[SERVER UPLOAD WARN] Msg ID "+messageId+" may exist without file."); }


        } finally {
            try {
                if (success) {
                    System.out.println("[SERVER UPLOAD NEW] Sending CMD_UPLOAD_SUCCESS");
                    dos.writeUTF("CMD_UPLOAD_SUCCESS");
                    dos.flush();

                    if (messageId != -1) {
                        if (isGroup) notifyGroupMembers(groupId, getGroupNameById(groupId), userAccount.getEmail(), messageId, userAccount.getId());
                        else notifyRecipientFileShared(recipientUserId, userDAO.getEmailById(recipientUserId), originalFileName, messageId);
                    }
                } else {
                    System.out.println("[SERVER UPLOAD NEW] Sending CMD_UPLOAD_ERROR");
                    dos.writeUTF("CMD_UPLOAD_ERROR:Upload failed on server side.");
                    dos.flush();
                }
            } catch (IOException ex) { System.err.println("[SERVER UPLOAD FINAL] Failed send status: " + ex.getMessage()); }
            System.out.println("[SERVER UPLOAD NEW] << Exiting receiveFileFromClientNew for ID: " + messageId);
        }
    }


    // --- Notifications & Utils (inchangés) ---
    private void notifyRecipientTextMessage(int rId, String rEmail, String msg, int msgId) { /* ... Code inchangé ... */ if(rEmail==null)return; String rl=rEmail.toLowerCase(); if(onlineUserStreams.containsKey(rl)){try{DataOutputStream d=onlineUserStreams.get(rl); String t=TIME_FORMATTER.format(java.time.LocalTime.now()); d.writeUTF(String.format("\n[%s] %s: %s\n> ",t,userAccount.getEmail(),msg));d.flush();}catch(IOException e){messageDAO.storePendingMessage(rId,msgId);onlineUserStreams.remove(rl);userDAO.setUserOnlineStatus(rId,false);}}else{messageDAO.storePendingMessage(rId,msgId);} }
    private void notifyGroupMembers(int gId, String gName, String sender, int msgId, int senderId) { /* ... Code inchangé ... */ Message m=messageDAO.getMessageById(msgId); if(m==null)return; List<Integer> mbrs=groupDAO.getGroupMembers(gId); String txt; String t=TIME_FORMATTER.format(java.time.LocalTime.now()); if("file".equals(m.getMessageType())){txt=String.format(AnsiColors.CYAN+"[Fichier] %s: %s (ID:%d)"+AnsiColors.RESET,sender,m.getFileName(),msgId);}else{txt=sender+": "+m.getMessage();} for(int mId:mbrs){if(mId==senderId)continue; String rE=userDAO.getEmailById(mId); if(rE!=null){String rL=rE.toLowerCase(); if(onlineUserStreams.containsKey(rL)){try{DataOutputStream d=onlineUserStreams.get(rL);d.writeUTF(String.format("\n[%s] [%s] %s\n> ",t,gName,txt));d.flush();}catch(IOException e){if(!"deleted".equals(m.getMessageType()))messageDAO.storePendingMessage(mId,msgId);onlineUserStreams.remove(rL);userDAO.setUserOnlineStatus(mId,false);}}else{if(!"deleted".equals(m.getMessageType()))messageDAO.storePendingMessage(mId,msgId);}}} }
    private void notifyOthersOfDeletion(int delMsgId) { /* ... Code inchangé ... */ Message dMsg=messageDAO.getMessageById(delMsgId);if(dMsg==null)return; List<Integer> targets=new ArrayList<>(); int gId=dMsg.getGroupId(); if(gId>0){List<Integer> mbrs=groupDAO.getGroupMembers(gId); for(int m:mbrs) if(m!=userAccount.getId())targets.add(m);}else{int rId=-1;if(dMsg.getReceiverEmail()!=null)rId=userDAO.getUserIdByEmail(dMsg.getReceiverEmail()); if(rId>0 && rId!=userAccount.getId())targets.add(rId);} String cmd="CMD_MSG_DELETED:"+delMsgId; for(int tId:targets){String tE=userDAO.getEmailById(tId); if(tE!=null){String tL=tE.toLowerCase(); if(onlineUserStreams.containsKey(tL)){try{onlineUserStreams.get(tL).writeUTF("\n"+cmd+"\n> ");onlineUserStreams.get(tL).flush();}catch(IOException e){onlineUserStreams.remove(tL);userDAO.setUserOnlineStatus(tId,false);}}}} }
    private void notifyRecipientFileShared(int rId, String rEmail, String fName, int msgId) { /* ... Code inchangé ... */ if(rEmail==null)rEmail=userDAO.getEmailById(rId); if(rEmail==null)return; String rL=rEmail.toLowerCase(); if(onlineUserStreams.containsKey(rL)){try{DataOutputStream d=onlineUserStreams.get(rL);String t=TIME_FORMATTER.format(java.time.LocalTime.now()); String n=String.format("\n[%s][File] %s: %s (ID:%d)\n -> Use view/download %d\n> ",t,userAccount.getEmail(),fName,msgId,msgId);d.writeUTF(n);d.flush();}catch(IOException e){messageDAO.storePendingMessage(rId,msgId);onlineUserStreams.remove(rL);userDAO.setUserOnlineStatus(rId,false);}}else{messageDAO.storePendingMessage(rId,msgId);} }
    private Message validateAndGetMessageForFileOps(int msgId) throws IOException { /* ... Code inchangé ... */ Message m=messageDAO.getMessageById(msgId); if(m==null){menuView.sendFeedback("ID "+msgId+" not found.",AnsiColors.RED);return null;} if("deleted".equals(m.getMessageType())){menuView.sendFeedback("ID "+msgId+" deleted.",AnsiColors.YELLOW);return null;} if(!"file".equals(m.getMessageType())){menuView.sendFeedback("ID "+msgId+" is not a file.",AnsiColors.RED);return null;} boolean ok=false; int gid=m.getGroupId(); if(gid<=0){int rid=-1;if(m.getReceiverEmail()!=null)rid=userDAO.getUserIdByEmail(m.getReceiverEmail()); ok=(m.getSenderId()==userAccount.getId())||(rid==userAccount.getId());}else{ok=groupDAO.getGroupMembers(gid).contains(userAccount.getId());} if(!ok){menuView.sendFeedback("Permission denied ID "+msgId,AnsiColors.RED);return null;} return m; }
    private File getFileFromMessage(Message msg) throws IOException { /* ... Code inchangé ... */ if(msg==null||!"file".equals(msg.getMessageType())||msg.getFileName()==null)return null; String sN=msg.getFileName().replaceAll("[^a-zA-Z0-9.\\-_ ]","_").trim(); if(sN.isEmpty())sN="file";
        File f = new File(AppPaths.SERVER_UPLOADS_DIR + msg.getMessageId() + "_" + sN); // <<< CHANGEMENT ICI
        if(!f.exists()){menuView.sendFeedback("Server file missing ID "+msg.getMessageId(),AnsiColors.RED);System.err.println("[FS ERR] Not found: "+f.getAbsolutePath());return null;} if(!f.isFile()){menuView.sendFeedback("Server path invalid ID "+msg.getMessageId(),AnsiColors.RED);System.err.println("[FS ERR] Not file: "+f.getAbsolutePath());return null;} if(!f.canRead()){menuView.sendFeedback("Server permission denied ID "+msg.getMessageId(),AnsiColors.RED);System.err.println("[FS ERR] No read: "+f.getAbsolutePath());return null;} return f; }
    private String getGroupNameById(int gId) { /* ... Code inchangé ... */ List<Group> uGrps=groupDAO.getGroupsForUser(userAccount.getId()); for(Group g:uGrps){if(g.getId()==gId)return g.getName();} return "Group "+gId; }

} // Fin ChatController