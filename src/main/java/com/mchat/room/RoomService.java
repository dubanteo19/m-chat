package com.mchat.room;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.mchat.model.Message;
import com.mchat.model.MessageReaction;
import com.mchat.model.MessageType;
import com.mchat.model.Room;
import com.mchat.model.RoomMember;
import com.mchat.model.User;
import com.mchat.notification.dto.response.PushRecipientInfo;
import com.mchat.room.dto.request.CreateRoomRequest;
import com.mchat.room.dto.request.MessagePaginationRequest;
import com.mchat.room.dto.request.PaginatedMessagesResponse;
import com.mchat.room.dto.response.MessageResponse;
import com.mchat.room.dto.response.ReactionResult;
import com.mchat.room.dto.response.RoomResponse;
import com.mchat.roommember.dto.response.RoomMemberInfo;
import com.mchat.user.UserService;

import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RoomService {
  @Inject
  UserService userService;
  @Inject
  JsonWebToken jwt;

  @WithTransaction
  public Uni<PaginatedMessagesResponse<MessageResponse>> getRoomMessagesPaginated(
      String roomId, MessagePaginationRequest pagination) {
    int limit = pagination.getLimit();
    var before = pagination.before();

    return Message.findByRoomPaginated(roomId, before, limit)
        .map(
            messages -> {
              boolean hasMore = messages.size() == limit;
              Instant nextCursor = messages.isEmpty() ? null : messages.getLast().sentAt;
              var responseMessages = new ArrayList<>(messages.stream().map(MessageResponse::from).toList());
              Collections.reverse(responseMessages);
              return new PaginatedMessagesResponse<>(responseMessages, nextCursor, hasMore);
            });
  }

  public Uni<Room> findRoomById(String roomId) {
    return Room.<Room>findById(roomId)
        .onItem()
        .ifNull()
        .failWith(() -> new IllegalArgumentException("Room not found: " + roomId));
  }

  @WithTransaction
  public Uni<Message> saveIncomingMessage(
      String roomId, String username, String content, MessageType messageType, Long parentId) {
    return Room.<Room>findById(roomId)
        .onItem()
        .ifNull()
        .failWith(() -> new IllegalArgumentException("Room not found: " + roomId))
        .chain(
            room -> User.findByUsername(username)
                .onItem()
                .ifNull()
                .failWith(() -> new IllegalArgumentException("User not found: " + username))
                .chain(
                    user -> {
                      if (parentId != null) {
                        return Message.<Message>findById(parentId)
                            .map(
                                parentMessage -> new Message(
                                    content, user, messageType, room, parentMessage));
                      } else {
                        return Uni.createFrom()
                            .item(new Message(content, user, messageType, room, null));
                      }
                    }))
        .chain(message -> message.persist());
  }

  @WithTransaction
  public Uni<Message> unsendMessage(Long messageId) {
    String username = jwt.getName();
    return Message.<Message>findById(messageId)
        .onItem()
        .ifNull()
        .failWith(() -> new IllegalArgumentException("Message not found: " + messageId))
        .chain(
            message -> {
              if (!message.sender.username.equals(username)) {
                return Uni.createFrom()
                    .failure(new SecurityException("Unauthorized context action"));
              }
              message.isDeleted = true;
              return message.persist();
            });
  }

  @WithTransaction
  public Uni<ReactionResult> saveReaction(
      String roomId, String username, Long messageId, String emoji) {
    return User.find("username = ?1", username)
        .<User>firstResult()
        .chain(
            user -> {
              if (user == null) {
                return Uni.createFrom()
                    .failure(new IllegalArgumentException("User not found: " + username));
              }
              return Message.<Message>findById(messageId)
                  .chain(
                      message -> {
                        if (message == null) {
                          return Uni.createFrom()
                              .failure(
                                  new IllegalArgumentException("Message not found: " + messageId));
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
                            // REMOVE ACTION
                            message.reactions.remove(managedReaction);
                            // Return null for reaction, but pass along the managed user object!
                            return Uni.createFrom().item(new ReactionResult(null, user));
                          } else {
                            // UPDATE ACTION
                            managedReaction.type = emoji;
                            managedReaction.reactedAt = Instant.now();
                            return managedReaction
                                .persist()
                                .map(v -> new ReactionResult(managedReaction, user));
                          }
                        } else {
                          // ADD ACTION
                          var newReaction = new MessageReaction(message, user, emoji);
                          message.reactions.add(newReaction);
                          return newReaction
                              .persist()
                              .map(v -> new ReactionResult(newReaction, user));
                        }
                      });
            });
  }

  @WithTransaction
  public Uni<RoomResponse> create(CreateRoomRequest request) {
    String username = jwt.getName();
    return userService
        .findByUsername(username)
        .chain(user -> Room.createAndJoin(request.name(), request.description(), user))
        .chain(room -> Uni.createFrom().item(RoomResponse.fromEntiy(room)));
  }

  @WithTransaction
  public Uni<List<RoomMemberInfo>> getRoomMembers(String roomId) {
    return RoomMember.findMembersByRoom(roomId)
        .map(members -> members.stream().map(RoomMemberInfo::fromEntity).toList());
  }

  @WithTransaction
  public Uni<List<RoomResponse>> findMyRooms() {
    String username = jwt.getName();
    return userService
        .findByUsername(username)
        .chain(user -> RoomMember.findRoomsByUser(user.id))
        .chain(
            rooms -> Uni.createFrom().item(rooms.stream().map(RoomResponse::fromEntiy).toList()));
  }

  /**
   * Caches all members with active push subscriptions for a room.
   */
  @CacheResult(cacheName = "room-push-recipients")
  @WithSession
  public Uni<List<PushRecipientInfo>> getRoomPushRecipients(String roomId) {
    return RoomMember.<RoomMember>find("FROM RoomMember rm JOIN FETCH rm.user u WHERE rm.room.id = ?1", roomId)
        .list()
        .map(members -> members.stream()
            .map(m -> m.user)
            .map(PushRecipientInfo::fromEntity)
            .filter(Objects::nonNull)
            .toList());
  }
}
