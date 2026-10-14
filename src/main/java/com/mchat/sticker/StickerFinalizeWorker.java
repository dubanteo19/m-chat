package com.mchat.sticker;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import com.mchat.event.StickerProcessedJob;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StickerFinalizeWorker {

    @Inject
    StickerService stickerService;

    @Incoming("finalize-requests")
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public Uni<Void> finalizeSticker(StickerProcessedJob job) {
        Log.infof("Finalizing sticker in DB: %s", job.stickerId());

        return stickerService.markAsReady(
                job.stickerId(),
                job.outputKey(),
                job.width(),
                job.height());
    }
}