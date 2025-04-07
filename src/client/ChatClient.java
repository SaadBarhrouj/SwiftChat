

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
            tcpSocket = new Socket(SERVER_ADDRESS, TCP_PORT);
            dis = new DataInputStream(tcpSocket.getInputStream());
            dos = new DataOutputStream(tcpSocket.getOutputStream());
            Thread listenerThread = new Thread(this::listenToServer);
            listenerThread.start();

            String userInput;
            while (running) {
                System.out.flush();
                userInput = scanner.nextLine();

                if (!running || dos == null) { // Vérifier si on est encore connecté/en cours d'exécution
                    break;
                }

                // Vérifier si la commande est un upload AVANT d'envoyer
                if (userInput.toLowerCase().startsWith("upload ")) {
                    // Si l'utilisateur tape "upload chemin/vers/fichier.txt"
                    handleUploadRequest(userInput);
                    // NE PAS envoyer la commande "upload ..." elle-même au serveur
                    // handleUploadRequest gère l'envoi de CMD_INITIATE_UPLOAD etc.
                } else {
                    // Envoyer les autres commandes/messages au serveur
                    dos.writeUTF(userInput);
                    dos.flush();
                    // Si la commande était "exit", "quit" ou une commande de déconnexion gérée côté serveur
                    // La boucle listenToServer devrait détecter la fermeture du socket et arrêter le client.
                    // Si on veut une fermeture explicite ici :
                    // if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit")) {
                    //     stop(); // Arrêter proprement le client
                    // }
                }
            }

        } catch (ConnectException e) {
            System.err.println("Impossible de se connecter au serveur : " + e.getMessage());
            System.err.println("Assurez-vous que le serveur est démarré et accessible à l'adresse " + SERVER_ADDRESS + ":" + TCP_PORT);
        } catch (UnknownHostException e) {
            System.err.println("Hôte inconnu : " + SERVER_ADDRESS);
        } catch (IOException e) {
            if (running) { // N'afficher l'erreur que si on ne s'attendait pas à s'arrêter
                System.err.println("Erreur d'E/S client lors de l'interaction principale : " + e.getMessage());
                // e.printStackTrace(); // Décommenter pour plus de détails si nécessaire
            }
        } finally {
            // S'assurer que stop() est appelé si ce n'est pas déjà fait
            if (running) {
                stop();
            }
            System.out.println("Boucle principale du client terminée.");
        }
    }


    private void listenToServer() {
        try {
            String messageFromServer;
            // Boucle tant que le client est en cours d'exécution
            while (running) {
                // Vérification si le flux d'entrée est toujours valide
                if (dis == null) {
                    System.err.println("Erreur: Flux d'entrée (dis) est null. Arrêt de l'écoute.");
                    running = false; // Arrêter le client
                    break; // Sortir de la boucle
                }

                // Lire le prochain message envoyé par le serveur
                messageFromServer = dis.readUTF(); // Bloquant jusqu'à réception
                // Nettoyer le message des espaces superflus pour certaines vérifications
                String trimmedMessage = messageFromServer.trim();

                // ========== ZONE CORRIGÉE ==========
                // Les commandes synchrones comme CMD_UPLOAD_READY, CMD_UPLOAD_SUCCESS, CMD_UPLOAD_ERROR
                // sont attendues et lues DIRECTEMENT par la méthode handleUploadRequest.
                // Ce thread d'écoute NE DOIT PAS les lire, sinon il les "vole" au thread principal.
                // Donc, on ne met PAS de 'if' pour ces commandes ici.

                // Gérer le début de la réception d'un fichier pour visualisation
                // Note: on utilise 'if' et non 'else if' car le bloc précédent a été retiré
                if (messageFromServer.startsWith("CMD_VIEW_FILE_START:")) {
                    // Appeler la méthode dédiée pour recevoir les données du fichier
                    handleFileViewFromServer(messageFromServer);
                    // handleFileViewFromServer gère l'affichage du prompt à la fin
                }
                // Gérer la notification de suppression de message
                else if (messageFromServer.startsWith("CMD_MSG_DELETED:")) {
                    try {
                        // Extraire l'ID du message supprimé
                        String commandPart = messageFromServer.substring("CMD_MSG_DELETED:".length());
                        // Enlever le prompt ">" qui pourrait être ajouté par le serveur
                        String msgIdStr = commandPart.replaceAll("\\s*>\\s*$", "").trim();
                        int deletedMsgId = Integer.parseInt(msgIdStr);
                        // Afficher l'info à l'utilisateur (avec une nouvelle ligne avant pour la clarté)
                        System.out.println("\n[INFO] Le message ID " + deletedMsgId + " a été supprimé par son expéditeur.");
                        // Ré-afficher le prompt après cette notification système
                        System.out.print("> "); System.out.flush();

                    } catch (Exception e) {
                        // Gérer les erreurs si la notification est mal formée
                        System.err.println("\n[Erreur Client] Notification de suppression mal form??e re??ue: " + messageFromServer);
                        // Ré-afficher le prompt même en cas d'erreur
                        System.out.print("> "); System.out.flush();
                    }
                }
                // Gérer les erreurs signalées par le serveur lors d'une tentative de view/download
                else if (messageFromServer.startsWith("CMD_VIEW_FILE_ERROR:") || messageFromServer.startsWith("CMD_DOWNLOAD_ERROR:")) {
                    // Afficher le message d'erreur venant du serveur
                    System.err.println("\nErreur signalée par le serveur: " + messageFromServer.substring(messageFromServer.indexOf(':') + 1));
                    // Ré-afficher le prompt après l'erreur
                    System.out.print("> "); System.out.flush();
                }
                // Cas par défaut : C'est probablement un message de chat, une info, un menu, etc.
                else {
                    // Afficher le message brut reçu du serveur
                    System.out.println(messageFromServer);

                    // Logique pour décider s'il faut ré-afficher le prompt "> "
                    // On ne le fait pas si le message serveur se termine déjà par un prompt (:) ou (>)
                    // ou si c'est une commande qui gère son propre prompt (comme VIEW_START ou MSG_DELETED).
                    if (!trimmedMessage.endsWith(":") && !trimmedMessage.endsWith(">") && !trimmedMessage.isEmpty()) {
                        // Vérifier qu'on n'est pas dans un cas déjà géré ci-dessus
                        if (!messageFromServer.startsWith("CMD_VIEW_FILE_START:") &&
                                !messageFromServer.startsWith("CMD_MSG_DELETED:") &&
                                !messageFromServer.startsWith("CMD_VIEW_FILE_ERROR:") &&
                                !messageFromServer.startsWith("CMD_DOWNLOAD_ERROR:"))
                        {
                            // Si c'est un message de chat ou une info simple, ré-afficher le prompt
                            System.out.print("> ");
                            System.out.flush();
                        }
                    }
                    // Si le message du serveur est une indication de déconnexion, arrêter le client
                    if (messageFromServer.contains("Deconnexion en cours...")) {
                        running = false; // Signal pour arrêter
                        // Pas besoin de break ici, la boucle vérifiera running à la prochaine itération ou une exception SocketException/EOFException sera levée
                    }
                }
                // La boucle continue pour attendre le prochain message serveur

                // ========== FIN ZONE CORRIGÉE ==========

            } // Fin de la boucle while(running)

        } catch (EOFException e) {
            // Le serveur a fermé la connexion normalement ou de manière inattendue (fin de flux)
            if (running) {
                System.out.println("\nDéconnecté du serveur (Fin de flux).");
            } else {
                // Si le client était déjà en train de s'arrêter (running = false), c'est normal
                System.out.println("\nFlux serveur fermé (attendu lors de l'arrêt).");
            }
            running = false; // Assurer que le client s'arrête
        } catch (SocketException e) {
            // Erreur de socket (connexion perdue, réinitialisée, etc.)
            if (running) {
                System.out.println("\nDéconnecté du serveur (Erreur Socket): " + e.getMessage());
                running = false;
            } else {
                // Le socket a été fermé par le client lui-même lors de l'arrêt
                System.out.println("\nSocket fermé côté client (attendu lors de l'arrêt).");
            }
        } catch (IOException e) {
            // Autre erreur d'entrée/sortie critique
            if (running) {
                System.err.println("\nErreur de lecture critique du serveur: " + e.getMessage());
                e.printStackTrace(); // Afficher la trace pour le débogage
                running = false;
            } else {
                // Erreur IO sur un flux déjà fermé pendant l'arrêt, souvent ignorable
                // System.err.println("\nIOException sur flux fermé (ignorable lors de l'arrêt): " + e.getMessage());
            }
        } catch (Exception e) {
            // Attraper toute autre exception inattendue
            if (running) {
                System.err.println("\nErreur inattendue dans le thread d'écoute: " + e.getMessage());
                e.printStackTrace();
                running = false;
            }
            // Ne rien afficher si l'erreur survient pendant l'arrêt normal
        } finally {
            // Ce bloc est exécuté à la fin du try ou après un catch, juste avant que le thread ne se termine
            if (!running) {
                // Si le thread s'est arrêté (suite à une erreur ou déconnexion)
                System.out.println("Thread d'écoute terminé. Tentative d'arrêt propre du client si nécessaire...");
                // Appeler stop() pour nettoyer les autres ressources (socket, flux, scanner)
                // seulement si ce n'est pas déjà en cours d'arrêt via le hook par exemple.
                // On peut ajouter une vérification atomique si nécessaire, mais stop() a déjà un check !running
                stop();
            }
            System.out.println("Fin du bloc finally du thread d'écoute."); // Log de fin du thread
        }
    } // Fin de la méthode listenToServer


    // Ancienne méthode handleFileUpload (potentiellement obsolète avec la nouvelle approche)
    // Gardée ici pour référence si besoin, mais la logique est dans handleUploadRequest
    /*
    private void handleFileUpload(int contactUserId, String contactNickname) {
        try {
            dos.writeUTF("upload"); // Cette commande n'est plus utilisée comme ça
            dos.flush();

            System.out.print("Entrez le chemin du fichier à envoyer: ");
            String filePath = scanner.nextLine();
            File file = new File(filePath);

            if (!file.exists() || !file.isFile()) {
                System.err.println("Fichier non trouvé ou invalide.");
                return;
            }

            dos.writeUTF(file.getName());
            dos.flush();
            dos.writeLong(file.length());
            dos.flush();

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                dos.flush();
            }

            String serverResponse = dis.readUTF();
            System.out.println(serverResponse);

        } catch (IOException e) {
            System.err.println("Erreur pendant l'envoi du fichier: " + e.getMessage());
        }
    }
    */

    void handleFileViewFromServer(String startCommand) {
        FileOutputStream fos = null;
        Path desktopPath = null;
        File localFile = null;
        long fileSize = -1;
        long bytesReceived = 0;
        boolean success = false;
        String fileName = "fichier_inconnu"; // Valeur par défaut

        try {
            // Extraire le nom du fichier de la commande
            fileName = startCommand.substring("CMD_VIEW_FILE_START:".length());
            if (fileName == null || fileName.trim().isEmpty()) {
                System.err.println("\nErreur: Nom de fichier invalide reçu du serveur.");
                // Essayer de consommer la taille et le reste pour ne pas désynchroniser
                try { fileSize = dis.readLong(); if (fileSize > 0) dis.skipBytes((int)Math.min(fileSize, 1024*1024)); dis.readUTF(); } catch (Exception ignored) {}
                System.out.print("> "); // Réafficher le prompt
                return;
            }
            // Nettoyer le nom de fichier (sécurité)
            fileName = fileName.replaceAll("[^a-zA-Z0-9.\\-_ ]", "_").trim();
            if (fileName.isEmpty()) fileName = "fichier_recu"; // Fallback

            // Lire la taille du fichier envoyée par le serveur
            fileSize = dis.readLong();
            System.out.println("\nRéception de '" + fileName + "' (" + fileSize + " octets) pour visualisation...");

            // Déterminer le chemin du bureau ou un dossier de fallback
            String userHome = System.getProperty("user.home");
            Path desktopDir = Paths.get(userHome, "Desktop");
            if (!Files.isDirectory(desktopDir)) {
                desktopDir = Paths.get(userHome, "Bureau"); // Essayer le nom français
                if (!Files.isDirectory(desktopDir)) {
                    desktopDir = Paths.get(userHome); // Fallback vers le dossier utilisateur
                    System.out.println("WARN: Impossible de trouver le dossier Bureau/Desktop. Sauvegarde dans : " + userHome);
                }
            }

            desktopPath = desktopDir.resolve(fileName);
            localFile = desktopPath.toFile();

            // Créer les répertoires parents si nécessaire
            Files.createDirectories(desktopPath.getParent());

            // Ouvrir le flux pour écrire le fichier localement
            fos = new FileOutputStream(localFile);
            byte[] buffer = new byte[8192];
            int bytesRead;

            // Boucle de réception des données
            while (bytesReceived < fileSize) {
                int toRead = (int) Math.min(buffer.length, fileSize - bytesReceived);
                bytesRead = dis.read(buffer, 0, toRead);

                if (bytesRead == -1) {
                    System.err.println("\nErreur: Connexion fermée prématurément par le serveur pendant le transfert.");
                    break; // Sortir de la boucle de réception
                }
                fos.write(buffer, 0, bytesRead);
                bytesReceived += bytesRead;
                // Affichage progression (optionnel)
                // System.out.print("\rReçu : " + bytesReceived + " / " + fileSize + " octets");
            }
            // System.out.println(); // Nouvelle ligne après progression

            // Lire la commande de fin ou d'erreur du serveur
            String endCommand = dis.readUTF();

            if ("CMD_VIEW_FILE_END".equals(endCommand)) {
                if(bytesReceived == fileSize) {
                    System.out.println("Fichier '" + fileName + "' reçu avec succès : " + desktopPath);
                    success = true;
                } else {
                    System.err.println("\nErreur: Transfert terminé par le serveur mais taille incorrecte reçue ("+bytesReceived+"/"+fileSize+")");
                }
            } else if (endCommand.startsWith("CMD_VIEW_FILE_ERROR:")) {
                System.err.println("\nErreur signalée par le serveur pendant le transfert: " + endCommand.substring("CMD_VIEW_FILE_ERROR:".length()));
            } else {
                System.err.println("\nErreur: Commande de fin inattendue reçue: " + endCommand);
                System.err.println("Reçu " + bytesReceived + " octets sur " + fileSize + " attendus.");
            }

        } catch (IOException e) {
            System.err.println("\nErreur d'E/S pendant la réception du fichier '" + fileName + "' : " + e.getMessage());
            // e.printStackTrace(); // Pour débogage
        } finally {
            // Fermer le flux d'écriture local
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    System.err.println("Erreur lors de la fermeture du fichier local '" + fileName + "' : " + e.getMessage());
                }
            }

            // Si succès, essayer d'ouvrir le fichier
            if (success && localFile != null && Files.exists(desktopPath)) {
                openFileOnDesktop(localFile);
            }
            // Si échec et fichier partiel existe, tenter de le supprimer
            else if (!success && localFile != null && Files.exists(desktopPath) && fileSize > 0 && bytesReceived < fileSize) {
                System.out.println("Tentative de suppression du fichier partiel...");
                if (localFile.delete()) {
                    System.out.println("Fichier partiel supprimé: " + desktopPath);
                } else {
                    System.err.println("Impossible de supprimer le fichier partiel: " + desktopPath);
                }
            }
            // Quoiqu'il arrive, réafficher le prompt
            System.out.print("> ");
            System.out.flush();
        }
    }


    private void openFileOnDesktop(File fileToOpen) {
        if (fileToOpen == null || !fileToOpen.exists()) {
            System.err.println("Erreur: Impossible d'ouvrir le fichier car il n'existe pas ou est invalide.");
            return;
        }

        // Vérifier si l'environnement supporte Desktop et l'action OPEN
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                try {
                    System.out.println("Tentative d'ouverture de : " + fileToOpen.getAbsolutePath());
                    desktop.open(fileToOpen); // Lancer l'ouverture
                    System.out.println("Commande d'ouverture envoyée au système.");
                } catch (IOException e) {
                    System.err.println("Erreur lors de la tentative d'ouverture du fichier : " + e.getMessage());
                    System.err.println("Vérifiez si une application est associée à ce type de fichier (. " + getFileExtension(fileToOpen) + ").");
                } catch (SecurityException e) {
                    System.err.println("Erreur de sécurité lors de la tentative d'ouverture : " + e.getMessage());
                } catch (UnsupportedOperationException e) {
                    System.err.println("L'ouverture de ce type de fichier n'est pas supportée : " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    System.err.println("Argument invalide pour l'ouverture (fichier inexistant?) : " + e.getMessage());
                }
            } else {
                System.err.println("L'action 'ouvrir' n'est pas supportée sur ce système via Java Desktop.");
                System.out.println("Le fichier est enregistré ici : " + fileToOpen.getAbsolutePath());
            }
        } else {
            System.err.println("La classe Java Desktop n'est pas supportée sur ce système.");
            System.out.println("Le fichier est enregistré ici : " + fileToOpen.getAbsolutePath());
        }
    }


    /**
     * Gère la demande d'envoi d'un fichier local au serveur lorsqu'un utilisateur
     * tape "upload chemin/vers/fichier".
     * Lit le fichier local et envoie ses données octet par octet APRÈS avoir
     * reçu la confirmation du serveur (CMD_UPLOAD_READY).
     * @param uploadCommand La commande complète tapée par l'utilisateur (ex: "upload C:\chemin\fichier.txt")
     */
    private void handleUploadRequest(String uploadCommand) {
        // Vérifications initiales
        if (dos == null || dis == null || !running) {
            System.err.println("Non connecté au serveur ou déconnexion en cours. Impossible d'envoyer le fichier.");
            return;
        }

        String localFilePath = "";
        try {
            // Extrait le chemin du fichier après "upload "
            localFilePath = uploadCommand.substring("upload ".length()).trim();
            // Si le chemin contient des guillemets (ex: "C:\Program Files\...") les enlever
            if (localFilePath.startsWith("\"") && localFilePath.endsWith("\"")) {
                localFilePath = localFilePath.substring(1, localFilePath.length() - 1);
            }

        } catch (IndexOutOfBoundsException e) {
            System.err.println("Commande invalide. Usage: upload chemin/vers/le/fichier/local.txt");
            System.out.print("> "); System.out.flush(); // Réafficher prompt
            return;
        }

        if (localFilePath.isEmpty()) {
            System.err.println("Chemin du fichier manquant. Usage: upload chemin/vers/le/fichier/local.txt");
            System.out.print("> "); System.out.flush(); // Réafficher prompt
            return;
        }

        File localFile = new File(localFilePath);
        // Valider le fichier local
        if (!localFile.exists()) {
            System.err.println("Erreur : Fichier local introuvable : " + localFilePath);
            System.out.print("> "); System.out.flush(); // Réafficher prompt
            return;
        }
        if (!localFile.isFile()) {
            System.err.println("Erreur : Le chemin spécifié n'est pas un fichier : " + localFilePath);
            System.out.print("> "); System.out.flush(); // Réafficher prompt
            return;
        }
        if (!localFile.canRead()) {
            System.err.println("Erreur : Impossible de lire le fichier local (permissions?) : " + localFilePath);
            System.out.print("> "); System.out.flush(); // Réafficher prompt
            return;
        }
        if (localFile.length() == 0) {
            System.out.println("Avertissement : Le fichier est vide. Envoi quand même...");
            // On pourrait choisir de refuser l'envoi de fichiers vides
            // System.err.println("Erreur : Le fichier local est vide.");
            // return;
        }


        FileInputStream fis = null;
        try {
            long fileSize = localFile.length();
            String fileName = localFile.getName();

            System.out.println("Préparation de l'envoi pour : '" + fileName + "' (" + fileSize + " octets)...");

            // --- DÉBUT DU DIALOGUE SYNCHRONE AVEC LE SERVEUR ---

            // 1. Envoyer l'intention d'upload et les métadonnées au serveur
            dos.writeUTF("CMD_INITIATE_UPLOAD"); // Signal pour le serveur
            dos.writeUTF(fileName);          // Envoyer nom du fichier
            dos.writeLong(fileSize);         // Envoyer taille du fichier
            dos.flush();                     // Important d'envoyer l'en-tête maintenant

            // 2. Attendre la réponse du serveur (prêt ou erreur)
            // C'EST ICI QUE CE THREAD ATTEND SPÉCIFIQUEMENT CMD_UPLOAD_READY
            System.out.println("Attente de la réponse du serveur...");
            String serverResponse = dis.readUTF(); // Bloquant

            // 3. Vérifier la réponse
            if (!serverResponse.startsWith("CMD_UPLOAD_READY:")) {
                // Le serveur a refusé ou a rencontré une erreur avant de commencer
                System.err.println("Le serveur a refusé ou rencontré une erreur : " + serverResponse);
                // Pas besoin de nettoyer le fichier local, on ne l'a pas encore lu
                System.out.print("> "); System.out.flush(); // Réafficher prompt
                return; // Arrêter l'upload
            }

            // Le serveur est prêt, extraire l'ID du message (optionnel côté client, mais bon à savoir)
            try {
                String messageIdStr = serverResponse.substring("CMD_UPLOAD_READY:".length());
                System.out.println("Serveur prêt (Message ID côté serveur: " + messageIdStr + "). Début du transfert...");
            } catch (Exception e) { // Si la réponse est mal formée
                System.out.println("Serveur prêt. Début du transfert..."); // Message générique
            }


            // 4. Lire le fichier local et envoyer les octets AU SERVEUR
            fis = new FileInputStream(localFile);
            byte[] buffer = new byte[8192]; // Buffer de 8 Ko
            int bytesRead;
            long totalSent = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                if (!running || dos == null) { // Vérifier si on est encore connecté
                    System.err.println("Déconnexion détectée pendant l'envoi du fichier.");
                    return; // Arrêter l'envoi
                }
                dos.write(buffer, 0, bytesRead); // Envoyer les octets lus au serveur
                totalSent += bytesRead;
                // Affichage de la progression (optionnel mais utile pour gros fichiers)
                System.out.print("\rEnvoyé : " + totalSent + " / " + fileSize + " octets");
            }
            dos.flush(); // S'assurer que les derniers octets sont envoyés
            System.out.println("\nFin de l'envoi des données au serveur."); // Nouvelle ligne après la progression


            // 5. Attendre la confirmation finale du serveur (succès ou erreur pendant SA réception)
            // C'EST ICI QUE CE THREAD ATTEND SPÉCIFIQUEMENT CMD_UPLOAD_SUCCESS ou CMD_UPLOAD_ERROR
            System.out.println("Attente de la confirmation finale du serveur...");
            String finalResponse = dis.readUTF(); // Bloquant

            if ("CMD_UPLOAD_SUCCESS".equals(finalResponse)) {
                System.out.println(">>> Fichier '" + fileName + "' envoyé avec succès au serveur !");
            } else {
                // Le serveur a signalé une erreur PENDANT ou APRES la réception des données
                System.err.println(">>> Échec de l'upload côté serveur : " + finalResponse);
            }

            // --- FIN DU DIALOGUE SYNCHRONE ---

        } catch (FileNotFoundException e) {
            // Devrait être déjà attrapé par les vérifications initiales, mais sécurité
            System.err.println("Erreur interne : Fichier non trouvé après vérification : " + e.getMessage());
        } catch (SocketException | EOFException e) {
            System.err.println("\nErreur réseau ou déconnexion pendant l'upload : " + e.getMessage());
            // La connexion est probablement perdue, on arrête le client
            stop();
        } catch (IOException e) {
            System.err.println("\nErreur d'E/S pendant l'upload : " + e.getMessage());
            e.printStackTrace(); // Pour le débogage
            // En cas d'erreur IO grave, on pourrait vouloir arrêter le client
            // stop();
        } finally {
            // Fermer le flux de lecture du fichier local quoiqu'il arrive
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    System.err.println("Erreur lors de la fermeture du fichier local : " + e.getMessage());
                }
            }
            // Le prompt ">" sera réaffiché par la boucle principale après le retour de cette méthode
            System.out.print("> ");
            System.out.flush();
        }
    }


    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // pas d'extension
        }
        return name.substring(lastIndexOf + 1).toLowerCase();
    }


    public void stop() {
        if (!running) return; // Déjà en cours d'arrêt ou arrêté
        running = false; // Signal pour arrêter les boucles

        System.out.println("\nDéconnexion du client en cours...");
        try {
            // Fermer le scanner pour débloquer la lecture dans la boucle principale si nécessaire
            if (scanner != null) {
                // System.out.println("Fermeture du scanner..."); // Debug
                scanner.close();
                // System.out.println("Scanner fermé."); // Debug
            }

            // Fermer les flux en premier (important !)
            // Mettre à null après fermeture pour éviter les utilisations ultérieures
            if (dos != null) {
                try {
                    // System.out.println("Fermeture du DataOutputStream..."); // Debug
                    dos.close();
                    // System.out.println("DataOutputStream fermé."); // Debug
                } catch (IOException e) { /* Ignorer les erreurs de fermeture */ }
                dos = null;
            }
            if (dis != null) {
                try {
                    // System.out.println("Fermeture du DataInputStream..."); // Debug
                    dis.close();
                    // System.out.println("DataInputStream fermé."); // Debug
                } catch (IOException e) { /* Ignorer les erreurs de fermeture */ }
                dis = null;
            }

            // Fermer le socket ensuite
            if (tcpSocket != null && !tcpSocket.isClosed()) {
                try {
                    // System.out.println("Fermeture du Socket TCP..."); // Debug
                    tcpSocket.close();
                    // System.out.println("Socket TCP fermé."); // Debug
                } catch (IOException e) { /* Ignorer les erreurs de fermeture */ }
                tcpSocket = null;
            }
            System.out.println("Client déconnecté et ressources libérées.");
        } catch (Exception e) { // Attraper toute exception pendant le nettoyage
            System.err.println("Erreur lors de la fermeture des ressources client: " + e.getMessage());
            // e.printStackTrace(); // Pour débogage
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();

        // Ajouter un hook pour gérer l'arrêt via Ctrl+C ou fermeture de la console
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nArrêt détecté (Shutdown Hook), nettoyage...");
            client.stop();
            System.out.println("Nettoyage terminé via Shutdown Hook.");
        }));

        // Démarrer le client
        client.start();

        // Ce message s'affiche si start() se termine (normalement ou à cause d'une erreur initiale)
        System.out.println("Le client a terminé son exécution principale (processus start() achevé).");
    }
}