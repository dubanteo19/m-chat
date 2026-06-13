package com.mchat.room;

import com.mchat.model.Message;
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
              Message message = new Message(content, sender, room);
              return message.persist();
            });
  }
}
