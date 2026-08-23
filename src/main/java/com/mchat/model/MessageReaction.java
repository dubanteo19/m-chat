package com.mchat.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "message_reactions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id", "reaction_type"}))
public class MessageReaction extends PanacheEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "message_id", nullable = false)
  public Message message;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", nullable = false)
  public User user;

  @Column(name = "reaction_type", nullable = false)
  public String type;

  public Instant reactedAt;

  public MessageReaction() {}

  public MessageReaction(Message message, User user, String type) {
    this.message = message;
    this.user = user;
    this.type = type;
    this.reactedAt = Instant.now();
  }
}

