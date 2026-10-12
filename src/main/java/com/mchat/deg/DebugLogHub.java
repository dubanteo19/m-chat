
package com.mchat.deg;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;

@ApplicationScoped
public class DebugLogHub {

    private static final int MAX_HISTORY = 500;

    @Context
    Sse sse;

    private final ArrayDeque<LogEntry> history = new ArrayDeque<>();
    private SseBroadcaster broadcaster;

    public record LogEntry(String time, String message) {
    }

    public synchronized void publish(String message) {
        LogEntry entry = new LogEntry(
                Instant.now().toString(),
                message);

        history.addLast(entry);

        while (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }

        if (broadcaster != null) {
            broadcaster.broadcast(
                    sse.newEventBuilder()
                            .name("log")
                            .data(String.class, toJson(entry))
                            .build());
        }
    }

    public synchronized List<LogEntry> history() {
        return List.copyOf(history);
    }

    public synchronized void register(
            jakarta.ws.rs.sse.SseEventSink sink) {
        if (broadcaster == null) {
            broadcaster = sse.newBroadcaster();
        }

        broadcaster.register(sink);
    }

    private String toJson(LogEntry entry) {
        return "{\"time\":\"" + escape(entry.time())
                + "\",\"message\":\"" + escape(entry.message()) + "\"}";
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}