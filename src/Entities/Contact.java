package Entities;

/**
 * La classe Contact représente un contact dans un système de gestion d'utilisateurs.
 * Elle contient des informations sur l'identifiant du contact, l'utilisateur propriétaire,
 * l'utilisateur contacté et le surnom donné au contact.
 */
public class Contact {

    /** Identifiant unique du contact */
    private int contactId;

    /** Identifiant de l'utilisateur propriétaire du contact */
    private int userId;

    /** Identifiant de l'utilisateur contacté */
    private int contactUserId;

    /** Surnom donné au contact */
    private String nickname;

    /**
     * Constructeur pour initialiser un objet Contact avec les informations de base.
     *
     * @param contactId L'identifiant unique du contact
     * @param userId L'identifiant de l'utilisateur propriétaire du contact
     * @param contactUserId L'identifiant de l'utilisateur contacté
     * @param nickname Le surnom donné au contact
     */
    public Contact(int contactId, int userId, int contactUserId, String nickname) {
        this.contactId = contactId;
        this.userId = userId;
        this.contactUserId = contactUserId;
        this.nickname = nickname;
    }

    /**
     * Récupère l'identifiant du contact.
     *
     * @return L'identifiant du contact
     */
    public int getContactId() {
        return contactId;
    }

    /**
     * Récupère l'identifiant de l'utilisateur propriétaire du contact.
     *
     * @return L'identifiant de l'utilisateur propriétaire
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Récupère l'identifiant de l'utilisateur contacté.
     *
     * @return L'identifiant de l'utilisateur contacté
     */
    public int getContactUserId() {
        return contactUserId;
    }

    /**
     * Récupère le surnom donné au contact.
     *
     * @return Le surnom du contact
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Définit un nouveau surnom pour le contact.
     *
     * @param nickname Le nouveau surnom à attribuer au contact
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
