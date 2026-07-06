package com.mchat.model;

import java.time.Instant;

import com.github.slugify.Slugify;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Room extends PanacheEntityBase {
  private static final Slugify SLUGIFY = Slugify.builder().build();
  @Id public String id;

  public String name;
  public String description;
  public Instant createdAt;

  public static Uni<Room> createAndJoin( String name, String description, User creator) {
    var room = Room.create(name, description);
    return room.<Room>persist()
        .chain(
            savedRoom -> {
              var master = new RoomMember(savedRoom, creator, RoomRole.MASTER);
              return master.persist().replaceWith(savedRoom);
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
}
