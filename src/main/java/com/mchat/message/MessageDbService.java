
package com.mchat.message;

import com.mchat.model.Message;
import com.mchat.model.MessageType;
import com.mchat.room.RoomDbService;
import com.mchat.user.UserDbService;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MessageDbService {
  @Inject
  RoomDbService roomDbService;

  @Inject
  UserDbService userDbService;

  @WithTransaction
  public Uni<Message> saveIncomingMessage(
      Long currentUserId, String roomId, String content, MessageType messageType, Long parentId) {
    return userDbService.findRequiredById(currentUserId)
        .chain(currentUser -> roomDbService.findRequiredById(roomId)
            .chain(room -> {
              Uni<Message> replyUni = parentId == null
                  ? Uni.createFrom().nullItem()
                  : findRequiredById(parentId);

              return replyUni.chain(replied -> {
                var message = new Message(
                    content,
                    currentUser,
                    messageType,
                    room,
                    replied);

                return message.persist();
              });
            }));

  }

  @WithSession
  public Uni<Message> findRequiredById(Long messageId) {
    return Message.<Message>findById(messageId)
        .onItem().ifNull()
        .failWith(() -> new IllegalArgumentException("Message not found: " + messageId));
  }

  @WithTransaction
  public Uni<Message> unsendMessage(Long userId, Long messageId) {
    return findRequiredById(messageId)
        .chain(message -> {
          if (!message.sender.id.equals(userId)) {
            return Uni.createFrom().failure(new SecurityException("Unauthorized context action"));
          }
          message.isDeleted = true;
          return message.persist();
        });
  }

  // @WithTransaction
  // public Uni<ReactionResult> saveReaction(String roomId, String username, Long
  // messageId, String emoji) {
  // return findMessageById(messageId)
  // .chain(message -> {
  // if (message == null) {
  // return Uni.createFrom().failure(new IllegalArgumentException("Message not
  // found: " + messageId));
  // }

  // if (message.reactions == null) {
  // message.reactions = new ArrayList<>();
  // }

  // MessageReaction managedReaction = message.reactions.stream()
  // .filter(r -> r.user != null && r.user.username.equals(username))
  // .findFirst()
  // .orElse(null);

  // if (managedReaction != null) {
  // if (managedReaction.type.equals(emoji)) {
  // message.reactions.remove(managedReaction);
  // return Uni.createFrom().item(new ReactionResult(null, user));
  // } else {
  // managedReaction.type = emoji;
  // managedReaction.reactedAt = Instant.now();
  // return roomDbService.persistReaction(managedReaction)
  // .map(v -> new ReactionResult(managedReaction, user));
  // }
  // } else {
  // var newReaction = new MessageReaction(message, user, emoji);
  // message.reactions.add(newReaction);
  // return roomDbService.persistReaction(newReaction)
  // .map(v -> new ReactionResult(newReaction, user));
  // }
  // });
  // };

}
