package com.mchat.room;

import com.mchat.model.Message;
import com.mchat.model.MessageType;
import com.mchat.model.Room;
import com.mchat.model.User;
import com.mchat.room.dto.request.MessagePaginationRequest;
import com.mchat.room.dto.request.PaginatedMessagesResponse;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;

@ApplicationScoped
public class RoomService {

  @WithTransaction
  public Uni<PaginatedMessagesResponse<Message>> getRoomMessagesPaginated(
      String roomId, MessagePaginationRequest pagination) {
    int limit = pagination.getLimit();
    var before = pagination.before();

    return Message.findByRoomPaginated(roomId, before, limit)
        .map(
            messages -> {
              boolean hasMore = messages.size() == limit;
              Instant nextCursor = messages.isEmpty() ? null : messages.getLast().sentAt;
              var responseMessages = new ArrayList<>(messages);
              Collections.reverse(responseMessages);
              return new PaginatedMessagesResponse<>(responseMessages, nextCursor, hasMore);
            });
  }

  @WithTransaction
  public Uni<Message> saveIncomingMessage(String roomId, String username, String content, MessageType messageType,
      Long parentId) {
    return Room.<Room>findById(roomId)
        .onItem().ifNull().failWith(() -> new IllegalArgumentException("Room not found: " + roomId))
        .chain(room ->
        User.findByUsername(username)
            .onItem().ifNull().failWith(() -> new IllegalArgumentException("User not found: " + username))
            .chain(user -> {
              if (parentId != null) {
                return Message.<Message>findById(parentId)
                    .map(parentMessage -> new Message(content, user, messageType, room, parentMessage));
              } else {
                return Uni.createFrom().item(new Message(content, user, messageType, room, null));
              }
            }))
        // Step 4: Persist the built message once all relationships are safely loaded
        .chain(message -> message.persist());
  }

  @WithTransaction
  public Uni<Message> unsendMessage(Long messageId, String username) {
    return Message.<Message>findById(messageId)
        .onItem().ifNull().failWith(() -> new IllegalArgumentException("Message not found: " + messageId))
        .chain(message -> {
          // Guardrail verification: Ensure user matches sender
          if (!message.sender.username.equals(username)) {
            return Uni.createFrom().failure(new SecurityException("Unauthorized context action"));
          }
          message.isDeleted = true;
          return message.persist();
        });
  }
}
