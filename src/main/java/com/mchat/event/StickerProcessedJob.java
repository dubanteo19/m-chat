package com.mchat.event;

public record StickerProcessedJob(Long stickerId, String outputKey, int width, int height) {
}