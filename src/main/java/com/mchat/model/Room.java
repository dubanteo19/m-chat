package com.mchat.model;

import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;

import com.github.slugify.Slugify;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Room extends PanacheEntityBase {
  private static final Slugify SLUGIFY = Slugify.builder().build();
  @Id
  public String id;

  public String name;
  public String description;
  public Instant createdAt;

  @Column(name = "last_seq", nullable = false)
  @ColumnDefault("0")
  public Long lastSeq = 0L;

  public boolean deleted = false;
  public Instant deletedAt;

  public static Uni<Room> createAndJoin(String name, String description, Long creatorId) {
    var room = Room.create(name, description);

    return room.<Room>persist()
        .chain(savedRoom -> {
          User userProxy = new User(creatorId);
          var master = new RoomMember(savedRoom, userProxy, RoomRole.MASTER);
          return master.persist().replaceWith(savedRoom);
        });
  }

  // --- Soft Delete Method ---
  public static Uni<Boolean> softDeleteRoom(String roomId) {
    return Room.<Room>findById(roomId)
        .chain(room -> {
          if (room == null || room.deleted) {
            return Uni.createFrom().item(false);
          }
          room.deleted = true;
          room.deletedAt = Instant.now();

          return Uni.createFrom().item(true);
        });
  }

  public static Room create(String name, String description) {
    Room room = new Room();
    room.id = SLUGIFY.slugify(name);
    room.name = name;
    room.description = description;
    room.createdAt = Instant.now();
    return room;
  }

  public static Uni<List<Room>> findRoomsByUser(Long userId) {
    return Room.<Room>find("""
        select r from Room r
        join RoomMember rm on rm.room = r
        where rm.user.id = ?1 and r.deleted = false
        """, userId)
        .list();
  }

  public static Uni<Long> incrementAndGetSeq(String roomId) {
    return update("lastSeq = lastSeq + 1 where id = ?1", roomId)
        .chain(() -> find("select r.lastSeq from Room r where r.id = ?1", roomId)
            .project(Long.class)
            .firstResult());
  }
}
