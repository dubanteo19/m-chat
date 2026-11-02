package com.mchat.sticker;

import com.mchat.sticker.dto.request.CreateStickerPackageRequest;
import com.mchat.sticker.dto.request.CreateStickerRequest;
import com.mchat.sticker.dto.response.StickerPackageResponse;
import com.mchat.storage.StorageService;
import com.mchat.storage.dto.PresignedUrl;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@ApplicationScoped
public class StickerPackageService {
  @Inject
  StickerPackageDbService stickerPackageDbService;

  @Inject
  StorageService storageService;

  public Uni<StickerPackageResponse> create(Long userId, CreateStickerPackageRequest request) {
    return stickerPackageDbService.create(userId, request).map(StickerPackageResponse::fromEntity);
  }

  public Uni<List<StickerPackageResponse>> getPackages(Long userId) {
    return stickerPackageDbService.getByUserId(userId);
  }

  public Uni<List<StickerPackageResponse>> getPublicPackages() {
    return stickerPackageDbService.getPublicPackages();
  }

  public Uni<Long> addSticker(Long userId, Long packageId, CreateStickerRequest request) {
    return stickerPackageDbService.addSticker(userId, packageId, request);
  }

  public Uni<Long> removeSticker(Long userId, Long packageId, Long stickerId) {
    return stickerPackageDbService.removeSticker(userId, packageId, stickerId);
  }

  public Uni<PresignedUrl> download(Long packageId) {
    return stickerPackageDbService
        .getStickerKeys(packageId)
        .chain(
            keys -> Uni.createFrom()
                .item(() -> generateZip(packageId, keys))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()));
  }

  public Uni<Long> deletePack(Long userId, Long packageId) {
    return stickerPackageDbService.deletePack(userId, packageId);
  }

  private PresignedUrl generateZip(Long stickerPackageId, List<String> stickerKeys) {
    String zipKey = "sticker-pack-archive/" + stickerPackageId + ".zip";
    Path tempFile = null;
    try {
      Log.infof("Creating zip package: %d temp", stickerPackageId);
      tempFile = Files.createTempFile("sticker-pack-", ".zip");
      try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempFile))) {
        for (String key : stickerKeys) {
          String fileName = Path.of(key).getFileName().toString();
          try (var fis = storageService.downloadObject(key)) {
            zos.putNextEntry(new ZipEntry(fileName));
            fis.transferTo(zos);
            zos.closeEntry();
          }
        }
      }

      Log.infof("upload zip package: %d to storage", stickerPackageId);
      storageService.uploadObject(tempFile.toString(), zipKey);
      Log.infof("generate PresignedUrl for sticker package %d", stickerPackageId);

      return storageService.createPresignedUploadUrl(zipKey);
    } catch (Exception e) {
      Log.error("Failed to donwload sticker pack");
      throw new RuntimeException("Failed to download sticker pack", e);
    } finally {
      if (tempFile != null) {
        try {
          Files.deleteIfExists(tempFile);
        } catch (IOException e) {
          Log.error("Failed to delete temp file");
        }
      }
    }
  }
}
