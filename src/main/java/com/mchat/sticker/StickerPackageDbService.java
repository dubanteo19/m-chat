package com.mchat.sticker;

import java.time.Instant;
import java.util.List;

import com.mchat.model.Sticker;
import com.mchat.model.StickerPackage;
import com.mchat.sticker.dto.request.CreateStickerPackageRequest;
import com.mchat.sticker.dto.request.CreateStickerRequest;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StickerPackageDbService {

  @WithTransaction
  public Uni<StickerPackage> create(Long userId, CreateStickerPackageRequest request) {
    var strickerPackage = new StickerPackage();
    strickerPackage.ownerId = userId;
    strickerPackage.name = request.name();
    strickerPackage.description = request.description();
    strickerPackage.visibility = request.visibility();
    strickerPackage.createdAt = Instant.now();
    strickerPackage.updatedAt = Instant.now();
    return strickerPackage.persist();
  }

  @WithSession
  public Uni<List<StickerPackage>> getByUserId(Long userId) {
    return StickerPackage.findByUserId(userId);
  }

  @WithTransaction
  public Uni<Long> addSticker(Long userId, Long packageId, CreateStickerRequest request) {
    return StickerPackage.findByIdAndOwnerId(packageId, userId)
        .onItem().ifNull().failWith(() -> new RuntimeException("Sticker package not found or does not belong to user"))
        .flatMap(stickerPackage -> {
          var sticker = new Sticker();
          sticker.packageId = packageId;
          sticker.createdAt = Instant.now();
          sticker.updatedAt = Instant.now();
          return sticker.persist().replaceWith(sticker.id);
        });
  }
}