package com.mchat.message;

import com.mchat.message.dto.response.ReactionResult;
import com.mchat.model.Message;
import com.mchat.model.MessageReaction;
import com.mchat.model.MessageType;
import com.mchat.model.Room;
import com.mchat.model.RoomMember;
import com.mchat.model.User;
import com.mchat.room.RoomDbService;
import com.mchat.user.UserDbService;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.logging.Logger;

@ApplicationScoped
public class MessageDbService {

    Logger logger = Logger.getLogger(MessageDbService.class.getName());
    @Inject
    RoomDbService roomDbService;

    @Inject
    UserDbService userDbService;

    @WithTransaction
    public Uni<Message> saveIncomingMessage(
            Long currentUserId, String roomId, String content, MessageType messageType, Long parentId) {

        return userDbService
                .findRequiredById(currentUserId)
                .chain(currentUser -> roomDbService.findRequiredById(roomId)
                        .chain(room -> {
                            Uni<Message> replyUni = parentId == null
                                    ? Uni.createFrom().<Message>nullItem()
                                    : findRequiredById(parentId);

                            return replyUni.chain(replied -> Room.incrementAndGetSeq(roomId)
                                    .chain(nextSeq -> {
                                        Message message = new Message(content, currentUser, messageType, room, replied,
                                                nextSeq);

                                        return message
                                                .<Message>persist()
                                                .call(savedMsg -> RoomMember.updateLastSeenSeq(roomId, currentUserId,
                                                        nextSeq));
                                    }));
                        }));
    }

    @WithSession
    public Uni<Message> findRequiredById(Long messageId) {
        return Message.<Message>findById(messageId)
                .onItem()
                .ifNull()
                .failWith(() -> new IllegalArgumentException("Message not found: " + messageId));
    }

    @WithSession
    public Uni<Message> findByIdAndRoomId(Long messageId, String roomId) {
        return Message.findByIdAndRoomId(messageId, roomId)
                .onItem()
                .ifNull()
                .failWith(
                        () -> new IllegalArgumentException(
                                String.format("Message %d is not found in room %s ", messageId, roomId)));
    }

    @WithTransaction
    public Uni<Message> unsendMessage(Long userId, Long messageId) {
        return findRequiredById(messageId)
                .chain(
                        message -> {
                            if (!message.sender.id.equals(userId)) {
                                return Uni.createFrom()
                                        .failure(new SecurityException("Unauthorized context action"));
                            }
                            message.isDeleted = true;
                            return message.persist();
                        });
    }

    @WithTransaction
    public Uni<ReactionResult> saveReaction(
            Long userId, String roomId, Long messageId, String emoji) {
        return userDbService
                .findRequiredById(userId)
                .chain(
                        user -> findByIdAndRoomId(messageId, roomId)
                                .chain(
                                        message -> {
                                            if (message.reactions == null)
                                                message.reactions = new ArrayList<>();
                                            var managedReaction = message.reactions.stream()
                                                    .filter(
                                                            r -> r.user != null
                                                                    && userId.equals(r.user.id)
                                                                    && emoji.equals(r.type))
                                                    .findFirst()
                                                    .orElse(null);

                                            if (managedReaction != null) {
                                                if (emoji.equals(managedReaction.type)) {
                                                    message.reactions.remove(managedReaction);
                                                    return Uni.createFrom().item(new ReactionResult(null, user));
                                                } else {
                                                    managedReaction.type = emoji;
                                                    managedReaction.reactedAt = Instant.now();
                                                    return Uni.createFrom()
                                                            .item(new ReactionResult(managedReaction, user));
                                                }
                                            }
                                            var newReaction = new MessageReaction(message, user, emoji);
                                            message.reactions.add(newReaction);
                                            return newReaction
                                                    .persist()
                                                    .map(v -> new ReactionResult(newReaction, user));
                                        }));
    }
}
