package com.mchat.room.dto.request;

import com.mchat.room.command.ReadRoomCommand;

public record ReadRoomRequest(
                Long seq) {
        public ReadRoomCommand toCommand(long userId, String roomId) {
                return new ReadRoomCommand(userId, roomId, seq);
        }
}