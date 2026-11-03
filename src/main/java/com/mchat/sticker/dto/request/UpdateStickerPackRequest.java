package com.mchat.sticker.dto.request;

import com.mchat.model.StickerPackageVisibility;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStickerPackRequest(
                @NotNull Long id,
                @Size(max = 50, message = "Name must be between 1 and 50 characters") String name,
                @Size(max = 500, message = "Description must not exceed 500 characters") String description,
                StickerPackageVisibility visibility) {
}