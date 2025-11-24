package br.edu.fatecgru.empresta;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Conversation {
    private String otherUserId;
    private String otherUserName;
    private String otherUserPhotoUrl;
    private String lastMessage;
    @ServerTimestamp
    private Date lastMessageTimestamp;

    public Conversation() {}

    // Getters and Setters
    public String getOtherUserId() { return otherUserId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }

    public String getOtherUserName() { return otherUserName; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }

    public String getOtherUserPhotoUrl() { return otherUserPhotoUrl; }
    public void setOtherUserPhotoUrl(String otherUserPhotoUrl) { this.otherUserPhotoUrl = otherUserPhotoUrl; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public Date getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(Date lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }
}
