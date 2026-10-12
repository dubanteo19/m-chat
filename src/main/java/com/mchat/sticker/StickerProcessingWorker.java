package com.mchat.sticker;

import com.mchat.event.StickerProcessingEvent;
import com.mchat.storage.StorageService;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StickerProcessingWorker {

    @Inject
    StorageService storageService;
    @Inject
    ImageProcessor imageProcessor;

    public void processSticker(StickerProcessingEvent event) {
        try (var inputStream = storageService.downloadObject(event.objectKey())) {
            var processedImage = imageProcessor.process(inputStream);
            String outputKey = "processed/" + event.stickerId() + ".webp";
            storageService.uploadObject(outputKey, processedImage.bytes());
            Log.infof("Successfully processed sticker %d and uploaded to %s", event.stickerId(), outputKey);
        } catch (Exception e) {
            Log.warnf("Could not process sticker %d", event.stickerId());
        }
    }
}
