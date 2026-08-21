package com.mchat.message.dto.response;

import java.time.Instant;

import com.mchat.socket.dto.EventType;

public record MessageDeleteResponse(
        EventType eventType,
        Long messageId,
        String deletedAt) {
    public static MessageDeleteResponse create(Long messageId) {
        return new MessageDeleteResponse(
                EventType.MESSAGE_DELETE,
                messageId,
                Instant.now().toString());
    }
}