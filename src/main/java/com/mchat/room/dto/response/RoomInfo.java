package com.mchat.room.dto.response;

import com.mchat.model.Room;
import com.mchat.model.RoomType;

public record RoomInfo(
    String id,
    String name,
    String description,
    RoomType type,
    Long lastSeq,
    long unreadCount,
    String peerDisplayName,
    String peerAvatarUrl) {

  public static RoomInfo fromEntity(Room room) {
    return new RoomInfo(room.id, room.name, room.description, room.type, room.lastSeq, 0,
        null, null);
  }
}
