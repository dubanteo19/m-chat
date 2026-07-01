package com.mchat.room.dto.response;

import com.mchat.auth.dto.response.UserInfo;
import com.mchat.model.Message;
import com.mchat.model.MessageType;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public record MessageResponse(
        Long id,
        MessageType type,
        UserInfo sender,
        String content,
        String sentAt,
        RepliedMessageInfo repliedTo,
        List<ReactionInfo> reactions,
        boolean isDeleted) {

    public record RepliedMessageInfo(Long id, String senderName, String content, MessageType type) {
    }

    public static MessageResponse from(Message message) {
        var senderInfo = UserInfo.fromEntity(message.sender);

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
        List<ReactionInfo> reactionInfos = (message.reactions == null)
                ? List.of()
                : message.reactions.stream().map(ReactionInfo::from).collect(Collectors.toList());

        return new MessageResponse(
                message.id,
                message.type,
                senderInfo,
                finalContent,
                message.sentAt != null ? message.sentAt.toString() : Instant.now().toString(),
                repliedInfo,
                reactionInfos,
                message.isDeleted);
    }

    public static MessageResponse createSystemMessage(String message) {
        var systemSender = new UserInfo("system", "System", "assets/system-avatar.png", "SERVER");

        return new MessageResponse(
                999999999L,
                MessageType.SYSTEM,
                systemSender,
                message,
                Instant.now().toString(),
                null,
                List.of(),
                false);
    }
}
