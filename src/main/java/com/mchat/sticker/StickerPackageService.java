package com.mchat.sticker;

import com.mchat.sticker.dto.request.CreateStickerPackageRequest;
import com.mchat.sticker.dto.response.StickerPackageResponse;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StickerPackageService {
    @Inject
    StickerPackageDbService stickerPackageDbService;

    public Uni<StickerPackageResponse> create(Long userId, CreateStickerPackageRequest request) {
        return stickerPackageDbService.create(userId, request).map(StickerPackageResponse::fromEntity);
    }
}