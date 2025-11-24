package br.edu.fatecgru.empresta;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String text;
    private boolean edited = false;
    private boolean deleted = false;
    @ServerTimestamp
    private Date timestamp;

    public ChatMessage() { }

    public ChatMessage(String senderId, String text) {
        this.senderId = senderId;
        this.text = text;
    }

    // Getters e Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
