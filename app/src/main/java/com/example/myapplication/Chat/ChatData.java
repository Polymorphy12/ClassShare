package com.example.myapplication.Chat;

public class ChatData {
    public String sessionId;
    public String userId;
    public String roomId;
    public String content;

    public ChatData(String sessionId, String userId, String roomId, String content) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.roomId = roomId;
        this.content = content;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
