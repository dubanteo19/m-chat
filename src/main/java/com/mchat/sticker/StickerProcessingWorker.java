package com.mchat.sticker;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import com.mchat.event.StickerProcessingJob;
import com.mchat.storage.StorageService;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
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

    // @Incoming("job-requests")
    // public Uni<Void> processSticker(Message<StickerProcessingJob> message) {
    //     var job = message.getPayload();
    //     Log.infof("Received sticker processing job: %s", job);
    //     return Uni.createFrom().item(() -> processStickerImage(job))
    //             .chain(processed -> stickerService.markAsReady(job.stickerId(), processed.outputKey(),
    //                     processed.width(), processed.height()))
    //             .replaceWithVoid();
    // }

    private ProcessedSticker processStickerImage(StickerProcessingJob job) {
        try (var input = storageService.downloadObject(job.objectKey())) {
            Log.infof("Processing sticker %s for package %s", job.stickerId(), job.packageId());
            var image = imageProcessor.process(input);

            String outputKey = "stickers/" + job.packageId() + "/" + job.stickerId() + ".webp";

            Log.infof("Uploading processed sticker %s to %s", job.stickerId(), outputKey);
            storageService.uploadObject(outputKey, image.bytes());

            return new ProcessedSticker(
                    outputKey, image.width(), image.height());
        } catch (Exception e) {
            Log.error("Failed to process sticker " + job.stickerId(), e);
            throw new RuntimeException(
                    "Failed to process sticker " + job.stickerId(), e);
        }
    }

    private record ProcessedSticker(String outputKey, int width, int height) {
    }
}