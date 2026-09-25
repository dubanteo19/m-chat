package com.mchat.room.command;

public record ReadRoomCommand(long userId, String roomId, long seq) {

}