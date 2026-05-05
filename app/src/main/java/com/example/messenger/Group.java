package com.example.messenger;

import java.util.HashMap;
import java.util.Map;

public class Group {

    private String id;
    private String title;
    private String description;
    private String adminId;
    private long createdAt;
    private Map<String, Boolean> members;

    public Group() {
        members = new HashMap<>();
    }

    public Group(String id, String title, String description, String adminId, long createdAt, Map<String, Boolean> members) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.adminId = adminId;
        this.createdAt = createdAt;
        this.members = members != null ? members : new HashMap<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, Boolean> getMembers() { return members; }
    public void setMembers(Map<String, Boolean> members) { this.members = members != null ? members : new HashMap<>(); }

    public boolean hasMember(String uid) {
        return uid != null && members != null && Boolean.TRUE.equals(members.get(uid));
    }
}
