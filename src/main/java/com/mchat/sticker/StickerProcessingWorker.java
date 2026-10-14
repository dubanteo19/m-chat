package com.mchat.sticker;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import com.mchat.event.StickerProcessedJob;
import com.mchat.event.StickerProcessingJob;
import com.mchat.storage.StorageService;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
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
    @Channel("sticker-processed-out")
    MutinyEmitter<StickerProcessedJob> jobEmitter;

    @Incoming("job-requests")
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public Uni<Void> processSticker(StickerProcessingJob job) {
        Log.infof("Processing image for sticker: %s", job.stickerId());
        return Uni.createFrom().item(() -> processStickerImage(job))
                .chain(processedSticker -> jobEmitter.send(
                        new StickerProcessedJob(
                                job.stickerId(),
                                processedSticker.outputKey(),
                                processedSticker.width(),
                                processedSticker.height())))
                .replaceWithVoid();
    }

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