package com.mchat.room.command;

public record CreateRoomCommand(
        long userId, String name, String description) {
}