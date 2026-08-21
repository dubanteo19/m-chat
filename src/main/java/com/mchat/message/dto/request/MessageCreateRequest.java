package com.mchat.message.dto.request;

import com.mchat.model.MessageType;

public record MessageCreateRequest(
        String content,
        Long parentId,
        MessageType type) {
}