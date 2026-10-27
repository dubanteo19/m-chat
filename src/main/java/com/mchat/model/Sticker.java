package com.mchat.model;

import java.time.Instant;
import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.smallrye.mutiny.Uni;
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

    @Column(nullable = false)
    public boolean deleted = false;

    @Column(name = "file_size")
    public Long fileSize;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    public static Uni<List<Sticker>> findByPackageId(Long packageId) {
        return find("packageId = ?1", packageId).list();
    }
}