package com.mchat.sticker.dto.response;

import com.mchat.model.StickerPackage;
import com.mchat.model.StickerPackageVisibility;

public record StickerPackageResponse(
        Long id,
        String name,
        String description,
        StickerPackageVisibility visibility
    ) {
    public static StickerPackageResponse fromEntity(StickerPackage stickerPackage) {
        return new StickerPackageResponse(
                stickerPackage.id,
                stickerPackage.name,
                stickerPackage.description,
                stickerPackage.visibility);
    }
}