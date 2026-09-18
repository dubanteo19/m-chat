package com.mchat.message.event;

import com.mchat.message.dto.response.MessageResponse;
import com.mchat.model.MessageType;

public record MessageCreatedEvent(
                Long messageId,
                String roomId,
                Long senderId,
                String senderDisplayname,
                String previewContent,
                MessageType type,
                Long seq) {
        public static MessageCreatedEvent toEvent(
                        MessageResponse message, String roomId) {
                return new MessageCreatedEvent(
                                message.id(),
                                roomId,
                                message.sender().id(),
                                message.sender().displayName(),
                                message.content(),
                                message.type(),
                                message.seq());
        }
}