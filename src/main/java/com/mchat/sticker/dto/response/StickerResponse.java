package com.mchat.sticker.dto.response;

import com.mchat.model.Sticker;
import com.mchat.storage.StorageUtil;

public record StickerResponse(
                long id,
                String url) {
        public static StickerResponse fromEntity(Sticker sticker) {
                String downloadUrl = StorageUtil.getUrlPrefix() + sticker.imageKey;
                return new StickerResponse(
                                sticker.id,
                                downloadUrl);
        }
}