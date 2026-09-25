package com.mchat.room.dto.request;

import com.mchat.room.command.CreateRoomCommand;

public record CreateRoomRequest(String name, String description) {
    public CreateRoomCommand toCommand(long userId) {
        return new CreateRoomCommand(userId, name, description);
    }
}
