package com.mchat.room.dto.response;

import com.mchat.model.Room;

public record RoomResponse(String id, String name) {
  public static RoomResponse fromEntiy(Room room) {
    return new RoomResponse(room.id, room.name);
  }
}
