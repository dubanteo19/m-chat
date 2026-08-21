
package com.mchat.message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

import com.mchat.message.dto.request.MessagePaginationRequest;
import com.mchat.message.dto.response.MessageDeleteResponse;
import com.mchat.message.dto.response.MessageResponse;
import com.mchat.message.dto.response.PaginatedMessagesResponse;
import com.mchat.model.MessageType;
import com.mchat.room.RoomDbService;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MessageService {
  Logger logger = Logger.getLogger(MessageService.class.getName());
  @Inject
  RoomDbService roomDbService;
  @Inject
  MessageDbService messageDbService;

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

  public Uni<MessageResponse> saveIncomingMessage(
      Long currentUserId, String roomId, String content, MessageType messageType, Long parentId) {
    return messageDbService.saveIncomingMessage(currentUserId, roomId, content, messageType, parentId)
        .map(MessageResponse::from);
  }

  public Uni<MessageDeleteResponse> unsendMessage(Long userId, Long messageId) {
    return messageDbService.unsendMessage(userId, messageId).map(message -> message.id)
        .map(MessageDeleteResponse::create);
  }

  // public Uni<ReactionResult> saveReaction(String roomId, String username, Long
  // messageId, String emoji) {
  // return messageDbService.saveReaction(roomId, username, messageId, emoji);
  // }

}
