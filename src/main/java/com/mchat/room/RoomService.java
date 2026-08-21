package com.mchat.room;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.mchat.common.cache.CacheConstants;
import com.mchat.model.RoomRole;
import com.mchat.notification.dto.response.PushRecipientInfo;
import com.mchat.room.dto.request.CreateRoomRequest;
import com.mchat.room.dto.response.RoomInfo;
import com.mchat.roommember.dto.response.RoomMemberInfo;
import com.mchat.user.UserService;

import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class RoomService {
  Logger logger = Logger.getLogger(RoomService.class.getName());
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

  public Uni<RoomInfo> create(Long userId, CreateRoomRequest request) {
    return roomDbService.createAndJoinRoom(request.name(), request.description(), userId)
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
    logger.info("CACHE MISS! Fetching from DB layer for roomId: " + roomId);
    return roomDbService.findMembersByRoom(roomId)
        .map(members -> members.stream().map(RoomMemberInfo::fromEntity).toList());
  }

  public Uni<List<RoomInfo>> findMyRooms(Long userId) {
    return roomDbService.findRoomsByUserId(userId)
        .map(rooms -> rooms.stream().map(RoomInfo::fromEntity).toList());
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
}
