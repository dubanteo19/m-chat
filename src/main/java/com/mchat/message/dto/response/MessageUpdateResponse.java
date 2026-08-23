package com.mchat.message.dto.response;

import com.mchat.auth.dto.response.UserInfo;
import com.mchat.model.MessageReaction;
import com.mchat.model.User;
import com.mchat.socket.dto.EventType;
import java.time.Instant;

public record MessageUpdateResponse(
    EventType eventType,
    Long messageId,
    String action, // "ADDED" or "REMOVED"
    ReactionInfo reaction) {

  public static MessageUpdateResponse createReactionUpdate(
      Long messageId, String emoji, MessageReaction reaction, User user) {

    var senderInfo = UserInfo.fromEntity(user);

    String timestamp =
        (reaction != null && reaction.reactedAt != null)
            ? reaction.reactedAt.toString()
            : Instant.now().toString();
    String actionStr = (reaction == null) ? "REMOVED" : "ADDED";
    Long reactionId = reaction == null ? null : reaction.id;
    var broadcastInfo = new ReactionInfo(reactionId, emoji, senderInfo, timestamp);

    return new MessageUpdateResponse(EventType.REACTION, messageId, actionStr, broadcastInfo);
  }
}
