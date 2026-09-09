package com.mchat.user.dto.event;

public record RoomUnreadUpdatedEvent(
                String roomId,
                Long seq) {
}