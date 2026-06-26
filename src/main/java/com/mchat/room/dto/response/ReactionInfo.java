package com.mchat.room.dto.response;

import java.time.Instant;
import com.mchat.model.MessageReaction;
import com.mchat.model.User;

public record ReactionInfo(
        String type,
        SenderInfo sender,
        String reactedAt) {

    public record SenderInfo(
            String username,
            String displayName,
            String avatarUrl,
            String title) {

        public static SenderInfo from(User user) {
            if (user == null)
                return null;
            return new SenderInfo(user.username, user.displayName, user.avatarUrl, user.title);
        }
    }

    public static ReactionInfo from(MessageReaction reaction) {
        if (reaction == null) {
            return null;
        }

        return new ReactionInfo(
                reaction.type,
                SenderInfo.from(reaction.user),
                reaction.reactedAt != null ? reaction.reactedAt.toString() : Instant.now().toString());
    }
}