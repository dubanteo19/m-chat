package com.mchat.sticker;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import com.mchat.event.StickerProcessedJob;

import io.quarkus.logging.Log;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StickerFinalizeWorker {
    @Inject
    Vertx vertx;

    @Inject
    StickerService stickerService;

    @Incoming("finalize-requests")
    public Uni<Void> finalizeSticker(StickerProcessedJob job) {
        Context current = Vertx.currentContext();
        if (current == null) {
            Log.errorf("No Vert.x context available for finalizing sticker: %s", job.stickerId());
            current = vertx.getOrCreateContext();
        }
        Context duplicated = ((ContextInternal) current).duplicate();
        Log.infof("Finalizing sticker in DB: %s", job.stickerId());
        return Uni.createFrom().emitter(emiteer -> duplicated.runOnContext(ignored -> {
            try {
                VertxContextSafetyToggle.setContextSafe(duplicated, true);
                Log.info("set context safe true");
                stickerService.markAsReady(job.stickerId(), job.outputKey(), job.width(), job.height())
                        .subscribe().with(ignoredResult -> emiteer.complete(null), emiteer::fail);
            } catch (Exception e) {
                Log.errorf(e, "Failed to toggle context safe for sticker: %s", job.stickerId());
            }
        })

        );

    }
}