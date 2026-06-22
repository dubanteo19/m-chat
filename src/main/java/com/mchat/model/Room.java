package com.mchat.model;

import java.time.Instant;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Room extends PanacheEntityBase {

  @Id
  public String id;

  public String name;
  public String description;
  public Instant createdAt;

  public static Room createNew(String id, String name, String description) {
    Room room = new Room();
    room.id = id.toLowerCase().trim().replace(" ", "-");
    room.name = name;
    room.description = description;
    room.createdAt = Instant.now();
    return room;
  }
}
