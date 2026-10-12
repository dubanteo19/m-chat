package com.mchat.sticker;

import com.mchat.event.StickerProcessingEvent;
import com.mchat.storage.StorageService;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class StickerProcessingWorker {

    @Inject
    StorageService storageService;
    @Inject
    ImageProcessor imageProcessor;
    @Inject
    StickerService stickerService;

    public void processSticker(@Observes StickerProcessingEvent event) {
        Log.infof("received event to process sticker %d with object key %s", event.stickerId(), event.objectKey());
        try (var inputStream = storageService.downloadObject(event.objectKey())) {
            var processedImage = imageProcessor.process(inputStream);
            String outputKey = "stickers/" + event.stickerId() + ".webp";
            storageService.uploadObject(outputKey, processedImage.bytes());
            Log.infof("Successfully processed sticker %d and uploaded to %s", event.stickerId(), outputKey);
            stickerService
                    .markAsReady(event.stickerId(), outputKey, processedImage.width(), processedImage.height())
                    .emitOn(Infrastructure.getDefaultWorkerPool())
                    .subscribe().with(
                            unused -> Log.infof("Marked sticker %d as ready", event.stickerId()),
                            failure -> Log.errorf(failure, "Failed to mark sticker %d as ready", event.stickerId()));

        } catch (Exception e) {
            Log.warnf("Could not process sticker %d", event.stickerId());
        }
    }
}
