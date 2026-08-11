package com.mchat.room.dto.response;

import com.mchat.model.Room;

public record RoomInfo(String id, String name, String description) {
  public static RoomInfo fromEntity(Room room) {
    return new RoomInfo(room.id, room.name, room.description);
  }
}
