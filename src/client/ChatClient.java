package client;

import java.net.*;
import java.io.*;
import java.util.Scanner;
import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ChatClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int TCP_PORT = 3001;
    private Socket tcpSocket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Scanner scanner;
    private volatile boolean running = true;

    public ChatClient() {
        scanner = new Scanner(System.in);
    }

    public void start() {
        try {
            System.out.println("Connecting to " + SERVER_ADDRESS + ":" + TCP_PORT + "...");
            tcpSocket = new Socket(SERVER_ADDRESS, TCP_PORT);
            dis = new DataInputStream(tcpSocket.getInputStream());
            dos = new DataOutputStream(tcpSocket.getOutputStream());
            System.out.println("Connected. Listener started.");
            Thread listenerThread = new Thread(this::listenToServer);
            listenerThread.setDaemon(true);
            listenerThread.start();

            String userInput;
            while (running) {
                System.out.flush();
                if (scanner.hasNextLine()) {
                    userInput = scanner.nextLine();
                } else {
                    break; // Scanner fermé
                }

                if (!running || dos == null) break;

                if (userInput.toLowerCase().startsWith("upload ")) {
                    handleUploadRequestNew(userInput); // Appel nouvelle méthode
                } else {
                    try {
                        dos.writeUTF(userInput);
                        dos.flush();
                    } catch (IOException e) {
                        if (running) { System.err.println("Error sending: " + e.getMessage()); stop(); }
                        break;
                    }
                }
            }
        } catch (ConnectException e) { System.err.println("\n[FATAL] Cannot connect: " + e.getMessage()); }
        catch (UnknownHostException e) { System.err.println("\n[FATAL] Unknown host: " + SERVER_ADDRESS); }
        catch (IOException e) { if (running) System.err.println("\n[FATAL] Client IO error: " + e.getMessage()); }
        catch (Exception e) { if (running) { System.err.println("\n[FATAL] Unexpected client error: " + e.getMessage()); e.printStackTrace(); } }
        finally {
            System.out.println("Main client loop ended. Cleaning up...");
            stop();
            System.out.println("Client shutdown.");
        }
    }

    private void listenToServer() {
        System.out.println("[Listener] Started.");
        try {
            String messageFromServer;
            while (running) {
                if (dis == null) { running = false; break; }
                messageFromServer = dis.readUTF();


                String trimmedMessage = messageFromServer.trim();
                System.out.println();
                if (messageFromServer.startsWith("CMD_VIEW_FILE_START:")) {
                    handleFileViewFromServer(messageFromServer);
                } else if (messageFromServer.startsWith("CMD_MSG_DELETED:")) {
                    try { String idStr = messageFromServer.substring(16).replaceAll("\\s*>\\s*$", "").trim(); System.out.println("[INFO] Message ID " + Integer.parseInt(idStr) + " deleted."); }
                    catch (Exception e) { System.err.println("[Listener] Bad delete format."); }
                    System.out.print("> "); System.out.flush();
                } else if (messageFromServer.startsWith("CMD_VIEW_FILE_ERROR:") || messageFromServer.startsWith("CMD_DOWNLOAD_ERROR:")) {
                    System.err.println("[SERVER ERROR] " + messageFromServer.substring(messageFromServer.indexOf(':') + 1)); System.out.print("> "); System.out.flush();
                } else if (messageFromServer.startsWith("CMD_UPLOAD_ERROR:")) { // Erreur finale upload reçue ici
                    System.err.println("[SERVER UPLOAD ERROR] " + messageFromServer.substring(17)); System.out.print("> "); System.out.flush();
                }
                else { // Message normal
                    System.out.println(messageFromServer);
                    if (!trimmedMessage.endsWith(":") && !trimmedMessage.endsWith(">") && !trimmedMessage.isEmpty() &&
                            !messageFromServer.startsWith("CMD_VIEW_FILE_START:") && !messageFromServer.startsWith("CMD_MSG_DELETED:") ) {
                        System.out.print("> "); System.out.flush();
                    }
                    if (messageFromServer.contains("Logging out...") || messageFromServer.contains("Déconnexion en cours...")) { running = false; }
                }
            }
        } catch (EOFException e) { if (running) System.out.println("\n[Listener] Disconnected (EOF)."); running = false; }
        catch (SocketException e) { if (running) System.out.println("\n[Listener] Disconnected (Socket Err): " + e.getMessage()); running = false; }
        catch (IOException e) { if (running) { System.err.println("\n[Listener] IO Error: " + e.getMessage()); running = false; } }
        catch (Exception e) { if (running) { System.err.println("\n[Listener] Unexpected Error: " + e.getMessage()); e.printStackTrace(); running = false; } }
        finally { System.out.println("[Listener] Thread finished."); }
    }

    private void handleUploadRequestNew(String uploadCommand) {
        System.out.println("[CLIENT UPLOAD NEW] Initiated...");
        if (dos == null || dis == null || !running) { System.err.println("Not connected."); return; }
        String localFilePath = "";
        try { localFilePath = uploadCommand.substring(7).trim(); if (localFilePath.startsWith("\"") && localFilePath.endsWith("\"")) localFilePath = localFilePath.substring(1, localFilePath.length() - 1); }
        catch (IndexOutOfBoundsException e) { System.err.println("Usage: upload /path/to/local/file.txt"); System.out.print("> "); System.out.flush(); return; }
        if (localFilePath.isEmpty()) { System.err.println("Missing file path."); System.out.print("> "); System.out.flush(); return; }
        File localFile = new File(localFilePath);
        if (!localFile.exists() || !localFile.isFile() || !localFile.canRead()) { System.err.println("Error accessing local file: " + localFilePath); System.out.print("> "); System.out.flush(); return; }

        FileInputStream fis = null;
        try {
            long fileSize = localFile.length(); String fileName = localFile.getName();
            System.out.println("Preparing upload: '" + fileName + "' (" + fileSize + " bytes)...");
            dos.writeUTF("CMD_INITIATE_UPLOAD"); dos.writeUTF(fileName); dos.writeLong(fileSize); dos.flush();
            System.out.println("[CLIENT] Sent INIT_UPLOAD & metadata. Sending file content...");
            fis = new FileInputStream(localFile); byte[] buffer = new byte[8192]; int bytesRead; long totalSent = 0;
            while ((bytesRead = fis.read(buffer)) != -1) {
                if (!running || dos == null) { System.err.println("Disconnected during send."); return; }
                dos.write(buffer, 0, bytesRead); totalSent += bytesRead;
                System.out.print("\rSent: " + totalSent + "/" + fileSize + " bytes");
            }
            dos.flush(); System.out.println("\n[CLIENT] Finished sending data ("+totalSent+" bytes).");
            System.out.println("[CLIENT] Waiting for final confirmation from server...");
            String finalResponse = dis.readUTF(); // Attend une seule réponse
            if ("CMD_UPLOAD_SUCCESS".equals(finalResponse)) { System.out.println(">>> File '" + fileName + "' sent successfully!"); }
            else if (finalResponse.startsWith("CMD_UPLOAD_ERROR:")) { System.err.println(">>> Server upload failed: " + finalResponse.substring(17)); }
            else { System.err.println(">>> Unexpected final response from server: " + finalResponse); }
        } catch (FileNotFoundException e) { System.err.println("File not found error: " + e.getMessage()); }
        catch (SocketException | EOFException e) { System.err.println("\nNetwork error/disconnect during upload: " + e.getMessage()); stop(); }
        catch (IOException e) { System.err.println("\nI/O error during upload: " + e.getMessage()); /*stop(); ?*/ }
        finally { if (fis != null) { try { fis.close(); } catch (IOException e) {} } System.out.print("> "); System.out.flush(); }
    }

    void handleFileViewFromServer(String startCommand) {
        FileOutputStream fos = null; Path desktopPath = null; File localFile = null;
        long fileSize = -1; long bytesReceived = 0; boolean success = false; String fileName = "unknown_file";
        try {
            fileName = startCommand.substring(22).replaceAll("[^a-zA-Z0-9.\\-_ ]", "_").trim();
            if (fileName.isEmpty()) fileName = "received_file";
            fileSize = dis.readLong();
            System.out.println("\nReceiving '" + fileName + "' (" + fileSize + " bytes)...");
            String userHome = System.getProperty("user.home"); Path desktopDir = Paths.get(userHome, "Desktop");
            if (!Files.isDirectory(desktopDir)) { desktopDir = Paths.get(userHome, "Bureau"); if (!Files.isDirectory(desktopDir)) desktopDir = Paths.get(userHome); }
            desktopPath = desktopDir.resolve(fileName); localFile = desktopPath.toFile();
            Files.createDirectories(desktopPath.getParent());
            fos = new FileOutputStream(localFile); byte[] buffer = new byte[8192]; int bytesRead;
            while (bytesReceived < fileSize && (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize - bytesReceived))) != -1) { fos.write(buffer, 0, bytesRead); bytesReceived += bytesRead; }
            String endCommand = dis.readUTF();

            if ("CMD_VIEW_FILE_END".equals(endCommand)) { if(bytesReceived == fileSize) { System.out.println("File '" + fileName + "' received: " + desktopPath); success = true; } else { System.err.println("\nError: Incorrect size ("+bytesReceived+"/"+fileSize+")"); } }
            else if (endCommand.startsWith("CMD_VIEW_FILE_ERROR:") || endCommand.startsWith("CMD_DOWNLOAD_ERROR:")) { System.err.println("\nServer error: " + endCommand.substring(endCommand.indexOf(':') + 1)); }
            else { System.err.println("\nError: Unexpected end command: " + endCommand); }
        } catch (IOException e) { System.err.println("\nI/O error receiving file '" + fileName + "': " + e.getMessage()); }
        finally {
            if (fos != null) try { fos.close(); } catch (IOException e) { System.err.println("Error closing local file: " + e.getMessage()); }
            if (success && localFile != null && Files.exists(desktopPath)) { openFileOnDesktop(localFile); }
            else if (!success && localFile != null && Files.exists(desktopPath)) { if (localFile.delete()) System.out.println("Partial file deleted."); else System.err.println("Failed delete partial file."); }

        }
    }

    private void openFileOnDesktop(File fileToOpen) {
        if (fileToOpen == null || !fileToOpen.exists()) { System.err.println("Cannot open file."); return; }
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try { System.out.println("Opening: " + fileToOpen.getAbsolutePath()); Desktop.getDesktop().open(fileToOpen); }
            catch (Exception e) { System.err.println("Error opening: " + e.getMessage()); System.out.println("File at: " + fileToOpen.getAbsolutePath()); }
        } else { System.out.println("Desktop not supported. File at: " + fileToOpen.getAbsolutePath()); }
    }

    private String getFileExtension(File file) {
        String name = file.getName(); int lastIndexOf = name.lastIndexOf(".");
        return (lastIndexOf == -1) ? "" : name.substring(lastIndexOf + 1).toLowerCase();
    }

    public void stop() {
        if (!running) return; running = false; System.out.println("\n[CLIENT] Shutting down...");
        try { if (scanner != null) scanner.close(); } catch (Exception e) {}
        try { if (dos != null) dos.close(); } catch (IOException e) {} dos = null;
        try { if (dis != null) dis.close(); } catch (IOException e) {} dis = null;
        try { if (tcpSocket != null && !tcpSocket.isClosed()) tcpSocket.close(); } catch (IOException e) {} tcpSocket = null;
        System.out.println("[CLIENT] Resources released.");
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { System.out.println("\nHook cleanup..."); client.stop(); System.out.println("Hook finished."); }));
        client.start();
        System.out.println("Main method finished.");
    }
}