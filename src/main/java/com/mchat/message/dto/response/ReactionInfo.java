
package com.mchat.message.dto.response;

import java.time.Instant;

import com.mchat.auth.dto.response.UserInfo;
import com.mchat.model.MessageReaction;

public record ReactionInfo(
        String type,
        UserInfo sender,
        String reactedAt) {

    public static ReactionInfo from(MessageReaction reaction) {
        if (reaction == null) {
            return null;
        }

        return new ReactionInfo(
                reaction.type,
                UserInfo.fromEntity(reaction.user),
                reaction.reactedAt != null ? reaction.reactedAt.toString() : Instant.now().toString());
    }
}