package com.mchat.roommember;

import com.mchat.model.Room;
import com.mchat.model.RoomMember;
import com.mchat.model.RoomRole;
import com.mchat.room.RoomService;
import com.mchat.user.UserService;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import java.util.List;

@ApplicationScoped
public class RoomMemberService {
  @Inject RoomService roomService;
  @Inject UserService userService;

  @WithSession
  public Uni<RoomMember> findMember(String roomId, Long userId) {
    return RoomMember.findMember(roomId, userId)
        .onItem()
        .ifNull()
        .failWith(() -> new IllegalArgumentException("User is not a member of this room"));
  }

  @WithTransaction
  public Uni<RoomMember> inviteUser(String roomId, String username) {
    return userService
        .findByUsername(username)
        .chain(
            user ->
                RoomMember.findMember(roomId, user.id)
                    .onItem()
                    .ifNotNull()
                    .failWith(() -> new IllegalArgumentException("User already in room"))
                    .chain(ignored -> roomService.findRoomById(roomId))
                    .chain(room -> new RoomMember(room, user, RoomRole.MEMBER).persist()));
  }

  @WithTransaction
  public Uni<Boolean> kickUser(String roomId, String targetUsername, String actorUsername) {
    return userService
        .findByUsername(actorUsername)
        .chain(actorUser -> findMember(roomId, actorUser.id))
        .chain(
            actor -> {
              if (actor.role != RoomRole.MASTER) {
                return Uni.createFrom()
                    .failure(new ForbiddenException("Only master can kick members from the room"));
              }
              return userService.findByUsername(targetUsername);
            })
        .chain(targetUser -> findMember(roomId, targetUser.id))
        .chain(targetMember -> targetMember.delete().replaceWith(true));
  }

  @WithSession
  public Uni<List<Room>> findRoomsByUsername(String username) {
    return userService.findByUsername(username).chain(user -> RoomMember.findRoomsByUser(user.id));
  }
}
