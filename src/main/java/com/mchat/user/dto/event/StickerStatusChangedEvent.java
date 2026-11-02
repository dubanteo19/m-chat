package com.mchat.user.dto.event;

public record StickerStatusChangedEvent(
                long stickerId,
                StickerStatus status) {
}