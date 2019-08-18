package com.example.myapplication.VideoList;

public class VideoRoomInfo {

    public String userId;
    public String roomName;
    public String roomId;

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public VideoRoomInfo(String userId, String roomId)
    {
        this.userId = userId;
        this.roomId = roomId;
    }
}
