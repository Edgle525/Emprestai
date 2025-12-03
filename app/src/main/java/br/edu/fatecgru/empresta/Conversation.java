package br.edu.fatecgru.empresta;

import java.util.Date;

public class Conversation {
    private String chatId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserPhotoUrl;
    private String lastMessage;
    private Date lastMessageTimestamp;
    private long unreadCount;

    public Conversation() {
        // Construtor vazio necessário para o Firestore
    }

    public Conversation(String otherUserId, String otherUserName, String otherUserPhotoUrl, String lastMessage) {
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.otherUserPhotoUrl = otherUserPhotoUrl;
        this.lastMessage = lastMessage;
    }

    // Getters e Setters
    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public String getOtherUserPhotoUrl() {
        return otherUserPhotoUrl;
    }

    public void setOtherUserPhotoUrl(String otherUserPhotoUrl) {
        this.otherUserPhotoUrl = otherUserPhotoUrl;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Date getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Date lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
