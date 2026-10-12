package com.mchat.sticker;

import com.mchat.model.Sticker;
import com.mchat.model.StickerStatus;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StickerDbService {

    @WithTransaction
    public Uni<Void> markReady(
            Long stickerId,
            String objectKey,
            int width,
            int height) {

        return findRequiredById(stickerId)
                .invoke(sticker -> {
                    sticker.imageKey = objectKey;
                    sticker.width = width;
                    sticker.height = height;
                    sticker.status = StickerStatus.READY;
                })
                .replaceWithVoid();
    }

    @WithSession
    public Uni<Sticker> findRequiredById(Long id) {
        return Sticker.<Sticker>findById(id)
                .onItem()
                .ifNull()
                .failWith(() -> new IllegalArgumentException("Sticker not found: " + id));
    }

}