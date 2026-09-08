package com.mchat.room.dto.response;

import com.mchat.model.Room;

public record RoomInfo(String id, String name, String description, long unreadCount) {
  public static RoomInfo fromEntity(Room room, long unreadCount) {
    return new RoomInfo(room.id, room.name, room.description, unreadCount);
  }

  public static RoomInfo fromEntity(Room room) {
    return new RoomInfo(room.id, room.name, room.description, 0);
  }
}
