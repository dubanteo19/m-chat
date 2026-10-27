package com.mchat.sticker;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mchat.model.Sticker;
import com.mchat.model.StickerPackage;
import com.mchat.model.StickerPackageVisibility;
import com.mchat.model.StickerStatus;
import com.mchat.sticker.dto.request.CreateStickerPackageRequest;
import com.mchat.sticker.dto.request.CreateStickerRequest;
import com.mchat.sticker.dto.response.StickerPackageResponse;
import com.mchat.sticker.dto.response.StickerResponse;

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
  public Uni<List<StickerPackageResponse>> getByUserId(Long userId) {
    return StickerPackage.<StickerPackage>find("ownerId = ?1 and deleted = false ORDER BY createdAt DESC", userId)
        .list()
        .chain(this::attachSticker);
  }

  @WithSession
  public Uni<StickerPackage> findByIdAndOwnerId(Long id, Long userId) {
    return StickerPackage.find("ownerId = ?1 and id = ?2 and deleted = false", userId, id).firstResult();
  }

  private Uni<List<StickerPackageResponse>> attachSticker(List<StickerPackage> packages) {
    if (packages.isEmpty())
      return Uni.createFrom().item(List.of());

    List<Long> packageIds = packages.stream()
        .map(p -> p.id)
        .toList();

    return Sticker.<Sticker>find(
        "packageId IN ?1 AND status = ?2 and deleted = false ORDER BY createdAt DESC",
        packageIds,
        StickerStatus.READY)
        .list()
        .map(stickers -> {
          Map<Long, List<StickerResponse>> stickersByPackageId = stickers.stream().collect(
              Collectors.groupingBy(
                  s -> s.packageId,
                  Collectors.mapping(
                      StickerResponse::fromEntity,
                      Collectors.toList())));

          return packages.stream()
              .map(p -> StickerPackageResponse.fromEntityWithStickers(p,
                  stickersByPackageId.getOrDefault(p.id, List.of())))
              .toList();
        });
  }

  @WithSession
  public Uni<List<StickerPackageResponse>> getPublicPackages() {
    return StickerPackage.<StickerPackage>find(
        "visibility = ?1 and deleted = false ORDER BY createdAt DESC",
        StickerPackageVisibility.PUBLIC)
        .list()
        .chain(this::attachSticker);
  }

  @WithTransaction
  public Uni<Long> addSticker(Long userId, Long packageId, CreateStickerRequest request) {
    return findByIdAndOwnerId(packageId, userId)
        .onItem().ifNull().failWith(() -> new RuntimeException("Sticker package not found or does not belong to user"))
        .flatMap(stickerPackage -> {
          var sticker = new Sticker();
          sticker.packageId = packageId;
          sticker.createdAt = Instant.now();
          sticker.updatedAt = Instant.now();
          return sticker.persist()
              .map(ignored -> sticker.id);
        });
  }

  @WithTransaction
  public Uni<Long> removeSticker(Long userId, Long packageId, Long stickerId) {
    return findByIdAndOwnerId(packageId, userId).flatMap(
        stickerPackage -> Sticker.<Sticker>find("id = ?1 and packageId = ?2 and deleted = false", stickerId, packageId)
            .firstResult()
            .onItem().ifNull().failWith(() -> new RuntimeException("Sticker not found"))
            .flatMap(sticker -> {
              sticker.deleted = true;
              return sticker.persist()
                  .replaceWith(sticker.id);
            }));
  }
}