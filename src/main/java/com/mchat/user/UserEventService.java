package com.mchat.user;

import com.mchat.room.RoomService;
import com.mchat.socket.ChatSocket;
import com.mchat.socket.UserEventBroadcaster;
import com.mchat.user.dto.event.RoomUnreadUpdatedEvent;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.logging.Logger;

@ApplicationScoped
public class UserEventService {
        Logger logger = Logger.getLogger(UserEventService.class.getName());
        public static final String ROOM_UPDATED = "ROOM_UPDATED";
        public static final String ROOM_UNREAD_UPDATED = "ROOM_UNREAD_UPDATED";
        @Inject
        UserEventBroadcaster broadcaster;
        @Inject
        RoomService roomService;

        public Uni<Void> notifyRoomUnreadChanged(
                        String roomId,
                        Long senderId,
                        long seq) {
                logger.info("Notifying room unread changed for roomId: " + roomId + ", senderId: " + senderId
                                + ", seq: " + seq);
                var roomMembers = roomService.getRoomMembers(roomId);
                return roomMembers
                                .invoke(members -> {
                                        logger.info("Processing unread changed notification for roomId: " + roomId + ", members: " + members);
                                        members.stream()
                                                        .filter(member -> !member.user().id().equals(senderId))
                                                        .filter(member -> !ChatSocket.isOnlineInRoom(
                                                                        roomId,
                                                                        member.user().username()))
                                                        .forEach(member -> broadcaster.send(
                                                                        member.user().id(),
                                                                        new RoomUnreadUpdatedEvent(roomId, seq)));
                                })
                                .replaceWithVoid();
        }
}