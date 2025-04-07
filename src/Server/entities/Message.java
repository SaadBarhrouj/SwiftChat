package Server.entities;

public class Message {
    private int messageId;
    private String senderEmail;
    private String receiverEmail;
    private String message;
    private String messageType;
    private String fileName;
    private String date;
    private int senderId;
    private int groupId;

    public Message(int messageId, int senderId, String senderEmail, String receiverEmail, String message, String messageType, String fileName, String date ,int groupId) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderEmail = senderEmail;
        this.receiverEmail = receiverEmail;
        this.message = message;
        this.messageType = messageType;
        this.fileName = fileName;
        this.date = date;
        this.groupId = groupId;

    }

    public int getSenderId() {
        return senderId;
    }

    public int getMessageId() {
        return messageId;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getReceiverEmail() {
        return receiverEmail;
    }

    public String getMessage() {
        return message;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDate() {
        // Retourne seulement l'heure (HH:mm:ss) au lieu de la date complète
        // Ajoutons une vérification pour éviter les erreurs si la date est nulle ou trop courte
        if (date != null && date.length() >= 19) { // Suppose un format comme "YYYY-MM-DD HH:mm:ss"
            return date.substring(11, 19); // Extrait HH:mm:ss
        } else if (date != null && date.matches("\\(\\d{2}:\\d{2}:\\d{2}\\)")) { // Si le format est (HH:mm:ss) comme vu dans le code serveur
            return date.substring(1, 9);
        }
        // Retourne la chaîne originale ou une chaîne vide si le format n'est pas reconnu
        return (date != null) ? date : "";
    }

    public int getGroupId() {
        return groupId;
    }
}