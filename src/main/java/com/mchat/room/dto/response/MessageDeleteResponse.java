package com.mchat.room.dto.response;

import java.time.Instant;

import com.mchat.socket.dto.EventType;

public record MessageDeleteResponse(
        EventType eventType,
        Long messageId,
        String deletedBy,
        String deletedAt) {
    public static MessageDeleteResponse create(Long messageId, String username) {
        return new MessageDeleteResponse(
                EventType.MESSAGE_DELETE,
                messageId,
                username,
                Instant.now().toString());
    }
}