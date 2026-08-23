package com.mchat.message.dto.response;

import com.mchat.auth.dto.response.UserInfo;
import com.mchat.model.MessageReaction;
import java.time.Instant;

public record ReactionInfo(Long id, String type, UserInfo sender, String reactedAt) {

  public static ReactionInfo from(MessageReaction reaction) {
    if (reaction == null) {
      return null;
    }

    return new ReactionInfo(
        reaction.id,
        reaction.type,
        UserInfo.fromEntity(reaction.user),
        reaction.reactedAt != null ? reaction.reactedAt.toString() : Instant.now().toString());
  }
}
