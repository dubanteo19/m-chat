package com.mchat.model;

import java.time.Instant;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "stickers")
public class Sticker extends PanacheEntity {

    @Column(name = "package_id", nullable = false)
    public Long packageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public StickerStatus status = StickerStatus.PROCESSING;

    @Column(name = "image_key", length = 500)
    public String imageKey;

    public Integer width;

    public Integer height;

    @Column(name = "file_size")
    public Long fileSize;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}