package com.mchat.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@WithTransaction
public class Message extends PanacheEntity {
  public String content;
  public String sender;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  @JsonIgnore
  public Room room;

  public Instant sentAt;
  public boolean isRead = false;

  public Message() {}

  public Message(String content, String sender, Room room) {
    this.content = content;
    this.sender = sender;
    this.room = room;
    this.sentAt = Instant.now();
  }

  public static Uni<List<Message>> findByRoomPaginated(String roomId, Instant before, int limit) {
    if (before == null) {
      return find("room.id = ?1 order by sentAt desc", roomId).page(0, limit).list();
    } else {
      return find("room.id = ?1 and sentAt < ?2 order by sentAt desc", roomId, before)
          .page(0, limit)
          .list();
    }
  }
}
