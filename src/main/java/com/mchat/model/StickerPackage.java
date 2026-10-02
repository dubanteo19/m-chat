package com.mchat.model;

import java.time.Instant;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "sticker_packages")
public class StickerPackage extends PanacheEntity {

    @Column(name = "owner_id", nullable = false)
    public Long ownerId;

    @Column(nullable = false, length = 50)
    public String name;

    @Column(length = 500)
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public StickerPackageVisibility visibility = StickerPackageVisibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public StickerPackageStatus status = StickerPackageStatus.DRAFT;

    @Column(nullable = false)
    public boolean deleted = false;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}