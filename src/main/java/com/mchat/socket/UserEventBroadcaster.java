package com.mchat.socket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.mchat.user.dto.event.UserEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

@ApplicationScoped
public class UserEventBroadcaster {
    @Inject
    Sse sse;
    Logger logger = Logger.getLogger(UserEventBroadcaster.class.getName());
    private final ConcurrentHashMap<Long, Set<SseEventSink>> connections = new ConcurrentHashMap<>();

    public void add(Long userId, SseEventSink sink) {
        connections
                .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(sink);
        logger.info("Added new connection for userId: " + userId + ", total connections: "
                + connections.get(userId).size());
    }

    public void remove(Long userId, SseEventSink sink) {
        var sinks = connections.get(userId);

        if (sinks != null) {
            sinks.remove(sink);

            if (sinks.isEmpty()) {
                connections.remove(userId);
            }
        }
    }

    public void send(Long userId, UserEvent userEvent) {
        var sinks = connections.get(userId);

        if (sinks == null)
            return;

        var event = sse.newEventBuilder()
                .name(userEvent.type())
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .data(userEvent.data())
                .build();

        for (var sink : sinks) {
            sink.send(event)
                    .exceptionally(ex -> {
                        sinks.remove(sink);
                        sink.close();
                        return null;
                    });
        }
    }
}