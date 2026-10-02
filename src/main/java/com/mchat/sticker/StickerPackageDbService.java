package com.mchat.sticker;

import java.time.Instant;

import com.mchat.model.StickerPackage;
import com.mchat.sticker.dto.request.CreateStickerPackageRequest;

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
}