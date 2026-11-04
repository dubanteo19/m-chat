package com.mchat.model;

import com.github.slugify.Slugify;
import com.mchat.room.dto.response.RoomInfo;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;

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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public RoomType type = RoomType.GROUP;

  public boolean deleted = false;
  public Instant deletedAt;

  public static Uni<Room> createAndJoin(String name, String description, Long creatorId) {
    var room = Room.create(name, description);

    return room.<Room>persist()
        .chain(
            savedRoom -> {
              User userProxy = new User(creatorId);
              var master = new RoomMember(savedRoom, userProxy, RoomRole.MASTER);
              return master.persist().replaceWith(savedRoom);
            });
  }

  public static Uni<Boolean> softDeleteRoom(String roomId) {
    return Room.<Room>findById(roomId)
        .chain(
            room -> {
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

  public static Uni<List<RoomInfo>> findByUserId(Long userId) {
    return Room.find("""
        select r.id,
               r.name,
               r.description,
               r.type,
               r.lastSeq,
               r.lastSeq - rm.lastSeenSeq,
               peerUser.displayName,
               peerUser.avatarUrl
        from Room r
        join RoomMember rm on rm.room = r
        left join RoomMember PeerRm
               on PeerRm.room = r
              and PeerRm.user.id <> ?1
              and r.type = 'DM'
        left join PeerRm.user peerUser
        where rm.user.id = ?1
          and r.deleted = false
        order by r.lastSeq desc
        """, userId)
        .project(RoomInfo.class)
        .list();
  }

  public static Uni<Room> findDmRoomBetweenUsers(Long userId1, Long userId2) {
    return Room.find("""
        select r
        from Room r
        join RoomMember rm1 on rm1.room = r and rm1.user.id = ?1
        join RoomMember rm2 on rm2.room = r and rm2.user.id = ?2
        where r.type = 'DM'
          and r.deleted = false
        """, userId1, userId2)
        .firstResult();
  }

  public static Uni<Long> incrementAndGetSeq(String roomId) {
    return update("lastSeq = lastSeq + 1 where id = ?1", roomId)
        .chain(
            () -> find("select r.lastSeq from Room r where r.id = ?1", roomId)
                .project(Long.class)
                .firstResult());
  }
}
