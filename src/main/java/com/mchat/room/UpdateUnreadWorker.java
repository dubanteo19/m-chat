package com.mchat.room;

import com.mchat.event.MessageCreatedEvent;
import com.mchat.socket.ChatSocket;
import com.mchat.socket.UserEventBroadcaster;
import com.mchat.user.UserEventService;
import com.mchat.user.dto.event.RoomUnreadUpdatedEvent;
import com.mchat.user.dto.event.UserEvent;
import com.mchat.user.dto.event.UserEventType;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

@ApplicationScoped
public class UpdateUnreadWorker {
    @Inject
    UserEventService userEventService;
    @Inject
    RoomCacheService roomCacheService;

    @Inject
    UserEventBroadcaster broadcaster;

    void onMessageCreated(@ObservesAsync MessageCreatedEvent event) {
        var members = roomCacheService.getCachedMembers(event.roomId());

        if (members == null) {
            Log.warnf("Room members not cached: %s", event.roomId());
            return;
        }

        members.stream()
                .filter(m -> !m.user().id().equals(event.senderId()))
                .forEach(m -> broadcaster.send(
                        m.user().id(),
                        UserEvent.of(UserEventType.ROOM_UNREAD_UPDATED,
                                new RoomUnreadUpdatedEvent(
                                        event.roomId(),
                                        event.seq()))));
    }

}