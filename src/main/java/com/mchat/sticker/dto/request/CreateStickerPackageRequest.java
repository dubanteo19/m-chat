package com.mchat.sticker.dto.request;

import com.mchat.model.StickerPackageVisibility;

public record CreateStickerPackageRequest(
        String name,
        String description,
        StickerPackageVisibility visibility) {
}