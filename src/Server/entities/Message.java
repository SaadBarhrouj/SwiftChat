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

    public Object getMessageId() {
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
        if (date != null && date.length() >= 19) {//2024-04-09T13:45:22 // format ISO 8601
            return date.substring(11, 19);
        } else if (date != null && date.matches("\\(\\d{2}:\\d{2}:\\d{2}\\)")) {
            return date.substring(1, 9);
        }
        return (date != null) ? date : "";
    }

    public int getGroupId() {
        return groupId;
    }
}