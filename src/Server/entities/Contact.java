package Server.entities;

import java.io.Serializable;

public class Contact implements Serializable {
    private static final long serialVersionUID = 1L;

    private int contactId;

    private int userId;

    private int contactUserId;

    private String nickname;

    public Contact(int contactId, int userId, int contactUserId, String nickname) {
        this.contactId = contactId;
        this.userId = userId;
        this.contactUserId = contactUserId;
        this.nickname = nickname;
    }

    public int getContactId() {
        return contactId;
    }

    public int getUserId() {
        return userId;
    }

    public int getContactUserId() {
        return contactUserId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}