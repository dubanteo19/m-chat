package com.mchat.sticker.dto.response;

import com.mchat.model.Sticker;
import com.mchat.storage.StorageUtil;

public record StickerReponse(
                long id,
                String url) {
        public static StickerReponse fromEntity(Sticker sticker) {
                String downloadUrl = StorageUtil.getUrlPrefix() + sticker.imageKey;
                return new StickerReponse(
                                sticker.id,
                                downloadUrl);
        }
}