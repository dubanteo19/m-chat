package com.mchat.message.dto.request;

import com.mchat.model.MessageType;

import jakarta.validation.constraints.Size;

public record MessageCreateRequest(
        @Size(max = 2000, message = "Message content must not exceed 2000 characters") String content, Long replyTo,
        MessageType type) {
}
