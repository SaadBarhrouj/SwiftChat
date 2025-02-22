
// Classe interne pour représenter un message
public  class Message {
    private String messageType;
    private String senderEmail;
    private String date;
    private String content;
    private String fileName;

    public Message(String messageType, String senderEmail, String date, String content, String fileName) {
        this.messageType = messageType;
        this.senderEmail = senderEmail;
        this.date = date;
        this.content = content;
        this.fileName = fileName;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getDate() {
        return date;
    }

    public String getContent() {
        return content;
    }

    public String getFileName() {
        return fileName;
    }
}