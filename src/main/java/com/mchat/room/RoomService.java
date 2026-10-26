package com.mchat.room;

import java.util.List;
import java.util.Objects;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.mchat.common.cache.CacheConstants;
import com.mchat.model.RoomRole;
import com.mchat.notification.dto.response.PushRecipientInfo;
import com.mchat.room.command.CreateRoomCommand;
import com.mchat.room.dto.request.ReadRoomRequest;
import com.mchat.room.dto.response.RoomInfo;
import com.mchat.roommember.dto.response.RoomMemberInfo;
import com.mchat.user.UserService;

import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class RoomService {
  @Inject
  RoomDbService roomDbService;
  @Inject
  UserService userService;
  @Inject
  JsonWebToken jwt;

  public Uni<RoomInfo> getRoomInfo(Long userId, String roomId) {
    return roomDbService.findMember(roomId, userId)
        .onItem().ifNull().failWith(() -> new ForbiddenException("User is not a member of the room: " + roomId))
        .chain(member -> fetchRoomInfoCached(roomId));
  }

  @CacheResult(cacheName = CacheConstants.ROOM_INFO)
  public Uni<RoomInfo> fetchRoomInfoCached(String roomId) {
    return roomDbService.findRequiredById(roomId)
        .map(RoomInfo::fromEntity);
  }

  public Uni<RoomInfo> create(CreateRoomCommand command) {
    return roomDbService.createAndJoinRoom(command)
        .map(RoomInfo::fromEntity);
  }

  public Uni<Boolean> delete(Long userId, String roomId) {
    return roomDbService.findMember(roomId, userId)
        .chain(member -> {
          if (member == null || member.role != RoomRole.MASTER) {
            throw new WebApplicationException("Only the room master can delete this room",
                Response.Status.FORBIDDEN);
          }
          return roomDbService.softDeleteRoom(roomId);
        });
  };

  @CacheResult(cacheName = "room-members")
  public Uni<List<RoomMemberInfo>> getRoomMembers(String roomId) {
    Log.infof("CACHE MISS! Fetching from DB layer for roomId: %s", roomId);
    return roomDbService.findMembersByRoom(roomId)
        .map(members -> members.stream().map(RoomMemberInfo::fromEntity).toList());
  }

  public Uni<List<RoomInfo>> findMyRooms(Long userId) {
    return roomDbService.findRoomsByUserId(userId);
  }

  @CacheResult(cacheName = "room-push-recipients")
  public Uni<List<PushRecipientInfo>> getRoomPushRecipients(String roomId) {
    return roomDbService.findPushRecipientMembers(roomId)
        .map(members -> members.stream()
            .map(m -> m.user)
            .map(PushRecipientInfo::fromEntity)
            .filter(Objects::nonNull)
            .toList());
  }

  public Uni<Void> read(Long userId, String roomId, ReadRoomRequest request) {
    return roomDbService.read(userId, roomId, request.seq());
  }
}
