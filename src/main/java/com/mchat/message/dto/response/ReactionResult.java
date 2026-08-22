package com.mchat.message.dto.response;

import com.mchat.model.MessageReaction;
import com.mchat.model.User;

public record ReactionResult(MessageReaction reaction, User user) {}
