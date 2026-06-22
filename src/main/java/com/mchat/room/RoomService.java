package com.mchat.room;

import com.mchat.model.Message;
import com.mchat.model.MessageType;
import com.mchat.model.Room;
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
  public Uni<Message> saveIncomingMessage(String roomId, String sender, String content) {
    return Room.<Room>findById(roomId)
        .onItem()
        .ifNull()
        .failWith(() -> new IllegalArgumentException("Room not found: " + roomId))
        .chain(
            room -> {
              var determinedType = determineMessageType(content);
              var message = new Message(content, sender, determinedType, room);
              return message.persist();
            });
  }

  private MessageType determineMessageType(String content) {
    if (content == null || content.isBlank()) {
      return MessageType.TEXT;
    }

    String lowerContent = content.toLowerCase().trim();
    if (lowerContent.matches(".*\\.(jpg|jpeg|png|gif|webp|svg)(\\?.*)?$")) {
      return MessageType.IMAGE;
    }
    if (lowerContent.matches(".*\\.(mp4|webm|ogg|mov)(\\?.*)?$")) {
      return MessageType.VIDEO;
    }
    return MessageType.TEXT;
  }
}
