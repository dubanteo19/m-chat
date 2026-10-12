package com.mchat.sticker;

import com.mchat.event.StickerProcessingEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class StickerProcessingWorker {
    void onStickerProcessing(@Observes StickerProcessingEvent event) {

    }
}