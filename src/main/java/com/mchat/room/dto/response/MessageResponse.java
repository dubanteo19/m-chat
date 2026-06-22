package com.mchat.room.dto.response;

import java.time.Instant;

import com.mchat.model.Message;
import com.mchat.model.MessageType;

public record MessageResponse(
                Long id,
                MessageType type,
                String sender,
                String content,
                String sentAt) {
        public static MessageResponse from(Message message) {
                return new MessageResponse(
                                message.id,
                                message.type,
                                message.sender,
                                message.content,
                                message.sentAt.toString());
        }

        public static MessageResponse createJoinMessage(String username) {
                return new MessageResponse(
                                999999999L,
                                MessageType.SYSTEM,
                                "System",
                                username + " joined the room.",
                                Instant.now().toString());
        }
}