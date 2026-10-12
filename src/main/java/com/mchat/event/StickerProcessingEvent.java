package com.mchat.event;

public record StickerProcessingEvent(
        Long stickerId,
        String objectKey) {
}