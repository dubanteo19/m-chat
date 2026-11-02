package com.mchat.sticker;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import com.mchat.event.StickerProcessedJob;
import com.mchat.event.StickerProcessingJob;
import com.mchat.socket.UserEventBroadcaster;
import com.mchat.storage.StorageService;
import com.mchat.user.dto.event.StickerStatus;
import com.mchat.user.dto.event.StickerStatusChangedEvent;
import com.mchat.user.dto.event.UserEvent;
import com.mchat.user.dto.event.UserEventType;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StickerProcessingWorker {

  @Inject
  StorageService storageService;
  @Inject
  ImageProcessor imageProcessor;
  @Inject
  StickerService stickerService;

  @Inject
  UserEventBroadcaster userEventBroadCaster;

  @Inject
  @Channel("sticker-processed-out")
  MutinyEmitter<StickerProcessedJob> jobEmitter;

  @Incoming("sticker-job-requests")
  @Blocking(value = "sticker-processing", ordered = false)
  public Uni<Void> processSticker(StickerProcessingJob job) {
    Log.infof("Received sticker processing request for sticker: %s", job.stickerId());

    userEventBroadCaster.send(
        job.userId(),
        new UserEvent(
            UserEventType.STICKER_STATUS_CHANGED,
            new StickerStatusChangedEvent(
                job.stickerId(), StickerStatus.PROCESSING)));

    return Uni.createFrom()
        .item(() -> processStickerImage(job))
        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
        .chain(
            processedSticker -> jobEmitter.send(
                new StickerProcessedJob(
                    job.stickerId(),
                    processedSticker.outputKey(),
                    processedSticker.width(),
                    processedSticker.height(),
                    job.userId())))
        .replaceWithVoid();
  }

  private ProcessedSticker processStickerImage(StickerProcessingJob job) {
    try (var input = storageService.downloadObject(job.objectKey())) {
      Log.infof(
          "Processing sticker %s for package %s object: %s",
          job.stickerId(), job.packageId(), job.objectKey());
      var image = imageProcessor.process(input);
      String outputKey = "stickers/" + job.packageId() + "/" + job.stickerId() + ".webp";
      Log.infof("Uploading processed sticker %s to %s", job.stickerId(), outputKey);
      storageService.uploadObject(outputKey, image.bytes());
      return new ProcessedSticker(outputKey, image.width(), image.height());
    } catch (Exception e) {
      Log.error("Failed to process sticker " + job.stickerId(), e);
      throw new RuntimeException("Failed to process sticker " + job.stickerId(), e);
    }
  }

  private record ProcessedSticker(String outputKey, int width, int height) {
  }
}
