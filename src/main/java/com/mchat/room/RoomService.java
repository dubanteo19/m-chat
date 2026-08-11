package com.mchat.room;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.mchat.model.Message;
import com.mchat.model.MessageReaction;
import com.mchat.model.MessageType;
import com.mchat.model.Room;
import com.mchat.model.RoomRole;
import com.mchat.notification.dto.response.PushRecipientInfo;
import com.mchat.room.dto.request.CreateRoomRequest;
import com.mchat.room.dto.request.MessagePaginationRequest;
import com.mchat.room.dto.request.PaginatedMessagesResponse;
import com.mchat.room.dto.response.MessageResponse;
import com.mchat.room.dto.response.ReactionResult;
import com.mchat.room.dto.response.RoomInfo;
import com.mchat.roommember.dto.response.RoomMemberInfo;
import com.mchat.user.UserService;

import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

  public Uni<PaginatedMessagesResponse<MessageResponse>> getRoomMessagesPaginated(
      String roomId, MessagePaginationRequest pagination) {
    int limit = pagination.getLimit();
    var before = pagination.before();

    return roomDbService.findMessagesPaginated(roomId, before, limit)
        .map(messages -> {
          boolean hasMore = messages.size() == limit;
          Instant nextCursor = messages.isEmpty() ? null : messages.getLast().sentAt;
          var responseMessages = new ArrayList<>(messages.stream().map(MessageResponse::from).toList());
          Collections.reverse(responseMessages);
          return new PaginatedMessagesResponse<>(responseMessages, nextCursor, hasMore);
        });
  }

  public Uni<Room> getRoomInfo(String roomId) {
    return roomDbService.findRoomById(roomId)
        .onItem().ifNull().failWith(() -> new IllegalArgumentException("Room not found: " + roomId));
  }

  public Uni<Room> findRoomById(String roomId) {
    return roomDbService.findRoomById(roomId)
        .onItem().ifNull().failWith(() -> new IllegalArgumentException("Room not found: " + roomId));
  }

  public Uni<Message> saveIncomingMessage(
      String roomId, String username, String content, MessageType messageType, Long parentId) {
    return findRoomById(roomId)
        .chain(room -> userService.findByUsername(username)
            .onItem().ifNull().failWith(() -> new IllegalArgumentException("User not found: " + username))
            .chain(user -> {
              if (parentId != null) {
                return roomDbService.findMessageById(parentId)
                    .map(parentMessage -> new Message(content, user, messageType, room, parentMessage));
              } else {
                return Uni.createFrom().item(new Message(content, user, messageType, room, null));
              }
            }))
        .chain(message -> roomDbService.persistMessage(message));
  }

  public Uni<Message> unsendMessage(Long messageId) {
    String username = jwt.getName();
    return roomDbService.findMessageById(messageId)
        .onItem().ifNull().failWith(() -> new IllegalArgumentException("Message not found: " + messageId))
        .chain(message -> {
          if (!message.sender.username.equals(username)) {
            return Uni.createFrom().failure(new SecurityException("Unauthorized context action"));
          }
          message.isDeleted = true;
          return roomDbService.persistMessage(message);
        });
  }

  @WithTransaction
  public Uni<ReactionResult> saveReaction(String roomId, String username, Long messageId, String emoji) {
    return userService.findByUsername(username)
        .chain(user -> {
          if (user == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("User not found: " + username));
          }
          return roomDbService.findMessageById(messageId)
              .chain(message -> {
                if (message == null) {
                  return Uni.createFrom().failure(new IllegalArgumentException("Message not found: " + messageId));
                }

                if (message.reactions == null) {
                  message.reactions = new ArrayList<>();
                }

                MessageReaction managedReaction = message.reactions.stream()
                    .filter(r -> r.user != null && r.user.username.equals(username))
                    .findFirst()
                    .orElse(null);

                if (managedReaction != null) {
                  if (managedReaction.type.equals(emoji)) {
                    message.reactions.remove(managedReaction);
                    return Uni.createFrom().item(new ReactionResult(null, user));
                  } else {
                    managedReaction.type = emoji;
                    managedReaction.reactedAt = Instant.now();
                    return roomDbService.persistReaction(managedReaction)
                        .map(v -> new ReactionResult(managedReaction, user));
                  }
                } else {
                  var newReaction = new MessageReaction(message, user, emoji);
                  message.reactions.add(newReaction);
                  return roomDbService.persistReaction(newReaction)
                      .map(v -> new ReactionResult(newReaction, user));
                }
              });
        });
  }

  public Uni<RoomInfo> create(CreateRoomRequest request) {
    String username = jwt.getName();
    return userService.findByUsername(username)
        .chain(user -> roomDbService.createAndJoinRoom(request.name(), request.description(), user))
        .map(RoomInfo::fromEntity);
  }

  public Uni<Boolean> delete(String roomId) {
    String username = jwt.getName();

    return userService.findByUsername(username)
        .chain(user -> {
          if (user == null) {
            throw new WebApplicationException("User not found", Response.Status.UNAUTHORIZED);
          }

          return roomDbService.findMember(roomId, user.id)
              .chain(member -> {
                if (member == null || member.role != RoomRole.MASTER) {
                  throw new WebApplicationException("Only the room master can delete this room",
                      Response.Status.FORBIDDEN);
                }
                return roomDbService.softDeleteRoom(roomId);
              });
        });
  }

  @CacheResult(cacheName = "room-members")
  public Uni<List<RoomMemberInfo>> getRoomMembers(String roomId) {
    logger.info("CACHE MISS! Fetching from DB layer for roomId: " + roomId);
    return roomDbService.findMembersByRoom(roomId)
        .map(members -> members.stream().map(RoomMemberInfo::fromEntity).toList());
  }

  public Uni<List<RoomInfo>> findMyRooms() {
    String username = jwt.getName();
    return userService.findByUsername(username)
        .chain(user -> roomDbService.findRoomsByUserId(user.id))
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
