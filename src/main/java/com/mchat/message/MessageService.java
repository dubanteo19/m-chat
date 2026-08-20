
package com.mchat.message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.mchat.model.Message;
import com.mchat.model.MessageReaction;
import com.mchat.room.RoomDbService;
import com.mchat.room.dto.request.MessagePaginationRequest;
import com.mchat.room.dto.request.PaginatedMessagesResponse;
import com.mchat.room.dto.response.MessageResponse;
import com.mchat.room.dto.response.ReactionResult;
import com.mchat.user.UserService;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MessageService {
  Logger logger = Logger.getLogger(MessageService.class.getName());
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

  // public Uni<Message> saveIncomingMessage(
  // String roomId, String username, String content, MessageType messageType, Long
  // parentId) {
  // return findRoomById(roomId)
  // .chain(room -> userService.findByUsername(username)
  // .onItem().ifNull().failWith(() -> new NotFoundException("User not found: " +
  // username))
  // .chain(user -> {
  // if (parentId != null) {
  // return roomDbService.findMessageById(parentId)
  // .map(parentMessage -> new Message(content, user, messageType, room,
  // parentMessage));
  // } else {
  // return Uni.createFrom().item(new Message(content, user, messageType, room,
  // null));
  // }
  // }))
  // .chain(message -> roomDbService.persistMessage(message));
  // }

  @WithTransaction
  public Uni<Message> unsendMessage(Long userId, Long messageId) {
    return roomDbService.findMessageById(messageId)
        .onItem().ifNull().failWith(() -> new IllegalArgumentException("Message not found: " + messageId))
        .chain(message -> {
          if (!message.sender.id.equals(userId)) {
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

}
