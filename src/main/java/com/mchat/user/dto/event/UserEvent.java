package com.mchat.user.dto.event;

public record UserEvent(
        String type,
        Object data) {
}