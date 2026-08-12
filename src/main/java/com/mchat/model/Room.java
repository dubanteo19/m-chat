package com.mchat.model;

import com.github.slugify.Slugify;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.List;

@Entity
public class Room extends PanacheEntityBase {
  private static final Slugify SLUGIFY = Slugify.builder().build();
  @Id
  public String id;

  public String name;
  public String description;
  public Instant createdAt;

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
}
