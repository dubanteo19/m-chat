package com.mchat.room.dto.response;

import com.mchat.model.Room;
import com.mchat.model.RoomType;
import com.mchat.storage.StorageUtil;

public record RoomInfo(
    String id,
    String name,
    String description,
    RoomType type,
    Long lastSeq,
    long unreadCount,
    String peerDisplayName,
    String peerAvatarUrl) {
  public RoomInfo(String id, String name, String description, RoomType type, Long lastSeq, long unreadCount,
      String peerDisplayName, String peerAvatarUrl) {
    String avatarUrl = StorageUtil.getUrlPrefix() + peerAvatarUrl;
    this.id = id;
    this.name = name;
    this.description = description;
    this.type = type;
    this.lastSeq = lastSeq;
    this.unreadCount = unreadCount;
    this.peerDisplayName = peerDisplayName;
    this.peerAvatarUrl = avatarUrl;
  }

  public static RoomInfo fromEntity(Room room) {
    return new RoomInfo(room.id, room.name, room.description, room.type, room.lastSeq, 0,
        null, null);
  }
}
