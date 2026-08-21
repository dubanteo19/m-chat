package com.mchat.room;

import java.time.Instant;
import java.util.List;

import com.mchat.model.Message;
import com.mchat.model.MessageReaction;
import com.mchat.model.Room;
import com.mchat.model.RoomMember;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import jakarta.ws.rs.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RoomDbService {

    @WithSession
    public Uni<List<RoomMember>> findPushRecipientMembers(String roomId) {
        return RoomMember.<RoomMember>find("FROM RoomMember rm JOIN FETCH rm.user u WHERE rm.room.id = ?1", roomId)
                .list();
    }

    @WithSession
    public Uni<List<RoomMember>> findMembersByRoom(String roomId) {
        return RoomMember.findMembersByRoom(roomId);
    }

    @WithSession
    public Uni<Room> findById(String roomId) {
        return Room.findById(roomId);
    }

    @WithSession
    public Uni<Room> findRequiredById(String roomId) {
        return Room.<Room>findById(roomId)
                .onItem().ifNull()
                .failWith(() -> new NotFoundException(
                        "Room not found: " + roomId));
    }

    @WithSession
    public Uni<List<Message>> findMessagesPaginated(String roomId, Instant before, int limit) {
        return Message.findByRoomPaginated(roomId, before, limit);
    }

    @WithSession
    public Uni<List<Room>> findRoomsByUserId(Long userId) {
        return Room.findRoomsByUser(userId);
    }

    @WithSession
    public Uni<RoomMember> findMember(String roomId, Long userId) {
        return RoomMember.<RoomMember>find("room.id = ?1 and user.id = ?2", roomId, userId).firstResult();
    }

    @WithTransaction
    public Uni<MessageReaction> persistReaction(MessageReaction reaction) {
        return reaction.persist();
    }

    @WithTransaction
    public Uni<Room> createAndJoinRoom(String name, String description, Long creatorId) {
        return Room.createAndJoin(name, description, creatorId);
    }

    @WithTransaction
    public Uni<Boolean> softDeleteRoom(String roomId) {
        return Room.softDeleteRoom(roomId);
    }
}