package com.mchat.event;

public record StickerProcessingJob(
                Long stickerId,
                Long packageId,
                String objectKey) {
}