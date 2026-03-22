package com.example.goodstart.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

public class Channel {
    private String id;
    private String name;
    private String description;
    private String createdBy;
    private String createdByName;
    private String inviteLink;
    private List<String> members;
    private String lastMessage;
    private @ServerTimestamp Date lastMessageTime;
    private @ServerTimestamp Date createdAt;
    private int membersCount;
    private boolean isPublic;

    public Channel() {}

    public Channel(String name, String description, String createdBy, String createdByName) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.isPublic = true;
        this.membersCount = 1;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public String getInviteLink() { return inviteLink; }
    public void setInviteLink(String inviteLink) { this.inviteLink = inviteLink; }
    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public Date getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(Date lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public int getMembersCount() { return membersCount; }
    public void setMembersCount(int membersCount) { this.membersCount = membersCount; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }
}
