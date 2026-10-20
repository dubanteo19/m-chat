package com.mchat.sticker;

import java.util.List;

import com.mchat.model.StickerPackage;
import com.mchat.sticker.dto.response.StickerReponse;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StickerService {
  @Inject
  StickerDbService stickerDbService;

  public Uni<Void> markAsReady(
      Long stickerId,
      String objectKey,
      int width,
      int height) {
    return stickerDbService.markReady(stickerId, objectKey, width, height);
  }

  public Uni<List<StickerReponse>> getStickersByPackageId(Long userId, Long packageId) {
    return StickerPackage.findByIdAndOwnerId(packageId, userId)
        .onItem().ifNull().failWith(() -> new RuntimeException("Sticker package not found or does not belong to user"))
        .chain(t -> stickerDbService.getByPackageId(packageId)
            .map(stickers -> stickers.stream().map(StickerReponse::fromEntity).toList()));
  }
}