package com.mchat.sticker;

import java.util.List;

import com.mchat.sticker.dto.request.CreateStickerPackageRequest;
import com.mchat.sticker.dto.request.CreateStickerRequest;
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

    public Uni<List<StickerPackageResponse>> getPackages(Long userId) {
        return stickerPackageDbService.getByUserId(userId)
                .map(list -> list.stream().map(StickerPackageResponse::fromEntity).toList());
    }

    public Uni<Long> addSticker(Long userId, Long packageId, CreateStickerRequest request) {
        return stickerPackageDbService.addSticker(userId, packageId, request);
    }
}