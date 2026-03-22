package com.example.goodstart.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Message {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_VOICE = "voice";

    private String id;
    private String senderId;
    private String senderName;
    private String text;
    private String type;
    private String mediaUrl;
    private long voiceDurationSec;
    private @ServerTimestamp Date timestamp;
    private boolean read;
    private String replyToId;
    private String replyToText;

    public Message() {}

    public Message(String senderId, String senderName, String text) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.type = TYPE_TEXT;
        this.read = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getType() { return type != null ? type : TYPE_TEXT; }
    public void setType(String type) { this.type = type; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public long getVoiceDurationSec() { return voiceDurationSec; }
    public void setVoiceDurationSec(long voiceDurationSec) { this.voiceDurationSec = voiceDurationSec; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public String getReplyToId() { return replyToId; }
    public void setReplyToId(String replyToId) { this.replyToId = replyToId; }
    public String getReplyToText() { return replyToText; }
    public void setReplyToText(String replyToText) { this.replyToText = replyToText; }
}
