package com.mchat.sticker.dto.response;

import com.mchat.model.Sticker;
import com.mchat.model.StickerStatus;
import com.mchat.storage.StorageUtil;

public record StickerResponse(
                long id,
                String url,
                StickerStatus status) {
        public static StickerResponse fromEntity(Sticker sticker) {
                String downloadUrl = StorageUtil.getUrlPrefix() + sticker.imageKey;
                return new StickerResponse(
                                sticker.id,
                                downloadUrl,
                                sticker.status);
        }
}