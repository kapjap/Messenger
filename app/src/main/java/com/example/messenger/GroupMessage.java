package com.example.messenger;

import java.util.HashMap;
import java.util.Map;

public class GroupMessage {

    private String id;
    private String text;
    private String senderId;
    private String senderName;
    private long timestamp;
    private Map<String, Boolean> readBy;

    public GroupMessage() {
        readBy = new HashMap<>();
    }

    public GroupMessage(String id, String text, String senderId, String senderName, long timestamp, Map<String, Boolean> readBy) {
        this.id = id;
        this.text = text;
        this.senderId = senderId;
        this.senderName = senderName;
        this.timestamp = timestamp;
        this.readBy = readBy != null ? readBy : new HashMap<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Map<String, Boolean> getReadBy() { return readBy; }
    public void setReadBy(Map<String, Boolean> readBy) { this.readBy = readBy != null ? readBy : new HashMap<>(); }

    public boolean isReadBy(String uid) {
        return uid != null && readBy != null && Boolean.TRUE.equals(readBy.get(uid));
    }
}
