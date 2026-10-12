package com.mchat.sticker;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StickerService {
    @Inject
    StickerDbService stickerDbService;

    public Uni<Void> markAsReady(
            Long stickerId,
            String objectKey,
            int width,
            int height) {
        return stickerDbService.markReady(stickerId, objectKey, width, height);
    }
}