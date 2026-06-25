package com.mchat.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
@WithTransaction
public class Message extends PanacheEntity {
  @Column(name = "content", columnDefinition = "TEXT")
  public String content;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", nullable = false)
  public User sender;

  @Enumerated(EnumType.STRING)
  @Column(name = "message_type")
  public MessageType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  @JsonIgnore
  public Room room;

  public Instant sentAt;
  public boolean isRead = false;
  public boolean isDeleted = false;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "parent_id")
  public Message parentMessage;

  @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  public List<MessageReaction> reactions = new ArrayList<>();

  public Message() {
  }

  public Message(String content, User sender, MessageType type, Room room, Message parentMessage) {
    this.content = content;
    this.sender = sender;
    this.type = type;
    this.room = room;
    this.parentMessage = parentMessage;
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
