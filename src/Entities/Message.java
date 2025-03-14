package Entities;

public class Message {
    private int messageId;
    private String senderEmail;
    private String receiverEmail;
    private String message;
    private String messageType;
    private String fileName;
    private String date;

    public Message(int messageId, String senderEmail, String receiverEmail, String message, String messageType, String fileName, String date) {
        this.messageId = messageId;
        this.senderEmail = senderEmail;
        this.receiverEmail = receiverEmail;
        this.message = message;
        this.messageType = messageType;
        this.fileName = fileName;
        this.date = date;
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
        return date;
    }

}