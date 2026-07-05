package com.mchat.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.List;

@Entity
@Table(
    name = "room_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
public class RoomMember extends PanacheEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  public Room room;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  public User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public RoomRole role;

  public Instant joinedAt;

  public RoomMember() {}

  public RoomMember(Room room, User user, RoomRole role) {
    this.room = room;
    this.user = user;
    this.role = role;
    this.joinedAt = Instant.now();
  }

  public static Uni<RoomMember> findMember(String roomId, Long userId) {
    return find("room_id = ?1 and user_id = ?2", roomId, userId).firstResult();
  }

  public static Uni<List<Room>> findRoomsByUser(Long userId) {
    return RoomMember.<RoomMember>find("user.id = ?1", userId)
        .list()
        .map(members -> members.stream().map((RoomMember member) -> member.room).toList());
  }
}
