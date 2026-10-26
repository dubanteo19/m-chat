package com.mchat.sticker.dto.response;

import java.util.List;

import com.mchat.model.StickerPackage;
import com.mchat.model.StickerPackageVisibility;

public record StickerPackageResponse(
        Long id,
        String name,
        String description,
        StickerPackageVisibility visibility,
        List<StickerResponse> stickers) {
    public static StickerPackageResponse fromEntity(StickerPackage stickerPackage) {
        var emptyStickers = List.<StickerResponse>of();
        return fromEntityWithStickers(stickerPackage, emptyStickers);
    }

    public static StickerPackageResponse fromEntityWithStickers(StickerPackage stickerPackage,
            List<StickerResponse> stickers) {
        return new StickerPackageResponse(
                stickerPackage.id,
                stickerPackage.name,
                stickerPackage.description,
                stickerPackage.visibility,
                stickers);
    }
}