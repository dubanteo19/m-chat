package com.mchat.room.dto.response;

import com.mchat.model.MessageReaction;
import com.mchat.model.User;

public record ReactionResult(MessageReaction reaction, User user) {
}