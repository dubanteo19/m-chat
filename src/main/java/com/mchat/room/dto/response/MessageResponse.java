package com.mchat.room.dto.response;

import java.time.Instant;
import com.mchat.model.Message;
import com.mchat.model.MessageType;

public record MessageResponse(
                Long id,
                MessageType type,
                SenderInfo sender,
                String content,
                String sentAt,
                boolean isDeleted,
                RepliedMessageInfo repliedTo) {

        public record SenderInfo(
                        String username,
                        String displayName,
                        String avatarUrl,
                        String title) {
        }

        public record RepliedMessageInfo(
                        Long id,
                        String senderName,
                        String content,
                        MessageType type) {
        }

        public static MessageResponse from(Message message) {
                SenderInfo senderInfo = new SenderInfo(
                                message.sender.username,
                                message.sender.displayName,
                                message.sender.avatarUrl,
                                message.sender.title);

                RepliedMessageInfo repliedInfo = null;
                if (message.parentMessage != null) {
                        String parentContent = message.parentMessage.isDeleted
                                        ? "This message was deleted."
                                        : message.parentMessage.content;

                        repliedInfo = new RepliedMessageInfo(
                                        message.parentMessage.id,
                                        message.parentMessage.sender.displayName,
                                        parentContent,
                                        message.parentMessage.type);
                }

                String finalContent = message.isDeleted ? "This message was deleted." : message.content;

                return new MessageResponse(
                                message.id,
                                message.type,
                                senderInfo,
                                finalContent,
                                message.sentAt != null ? message.sentAt.toString() : Instant.now().toString(),
                                message.isDeleted,
                                repliedInfo);
        }

        public static MessageResponse createJoinMessage(String username) {
                var systemSender = new SenderInfo("system", "System", "assets/system-avatar.png", "SERVER");

                return new MessageResponse(
                                999999999L,
                                MessageType.SYSTEM,
                                systemSender,
                                username + " joined the room.",
                                Instant.now().toString(),
                                false,
                                null);
        }
}