import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class UserEventBroadcaster {
    @Inject
    Sse sse;
    private final ConcurrentHashMap<Long, Set<SseEventSink>> connections = new ConcurrentHashMap<>();

    public void add(Long userId, SseEventSink sink) {
        connections
                .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(sink);
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

    public void send(Long userId, Object data) {
        var sinks = connections.get(userId);

        if (sinks == null) {
            return;
        }

        OutboundSseEvent event = sse.newEventBuilder()
                .name("room-message")
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .data(data)
                .build();

        for (var sink : sinks) {
            sink.send(event)
                    .exceptionally(ex -> {
                        return null;
                    });
        }
    }
}