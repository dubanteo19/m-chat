package com.mchat.room.dto.response;

import java.time.Instant;
import com.mchat.model.MessageReaction;
import com.mchat.model.User;

public record MessageUpdateResponse(
        String type, // "REACTION"
        Long messageId,
        String action, // "ADDED" or "REMOVED"
        ReactionBroadcastInfo reaction) {
    public record ReactionBroadcastInfo(
            String type, // This holds the emoji character
            MessageResponse.SenderInfo sender,
            String reactedAt) {
    }

    // Factory method to transform your domain results cleanly
    public static MessageUpdateResponse createReactionUpdate(
            Long messageId,
            String emoji,
            MessageReaction reaction,
            User user) {

        var senderInfo = new MessageResponse.SenderInfo(
                user.username,
                user.displayName,
                user.avatarUrl,
                user.title);

        String timestamp = (reaction != null && reaction.reactedAt != null)
                ? reaction.reactedAt.toString()
                : Instant.now().toString();

        var broadcastInfo = new ReactionBroadcastInfo(emoji, senderInfo, timestamp);
        String actionStr = (reaction == null) ? "REMOVED" : "ADDED";

        return new MessageUpdateResponse("REACTION", messageId, actionStr, broadcastInfo);
    }
}