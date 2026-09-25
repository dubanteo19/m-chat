package com.mchat.room.command;

public record DeleteRoomCommand(
                long userId, String roomId) {
}