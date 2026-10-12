package com.mchat.sticker;

import com.mchat.storage.StorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StickerWorker {
  @Inject StorageService storageService;
  @Inject ImageProcessor imageProcessor;

  public record StickerJob(Long id, String objectKey) {}

  public void processSticker(StickerJob job) {
    try (var inputStream = storageService.downloadObject(job.objectKey())) {
      var processedImage = imageProcessor.process(inputStream);
      String outputKey = "processed/" + job.id() + ".webp";
      storageService.uploadObject(outputKey, processedImage.bytes());
    } catch (Exception e) {
      LOG.warning("Could not process sticker " + job.id());
    }
  }
}
