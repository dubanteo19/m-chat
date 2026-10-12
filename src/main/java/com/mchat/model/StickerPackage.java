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

    public static Uni<List<StickerPackage>> findByUserId(Long userId) {
        return StickerPackage.find("ownerId = ?1 and deleted = false", userId).list();
    }

    public static Uni<StickerPackage> findByIdAndOwnerId(Long id, Long userId) {
        return StickerPackage.find("ownerId = ?1 and id = ?2 and deleted = false", userId, id).firstResult();
    }
}