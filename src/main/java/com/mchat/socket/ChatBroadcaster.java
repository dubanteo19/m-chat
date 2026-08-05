package com.mchat.socket;

import java.util.List;

import io.quarkus.websockets.next.OpenConnections;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ChatBroadcaster {
    @Inject
    OpenConnections connections;

    public <M> Uni<Void> sendToRoom(String roomId, M payload) {

        List<Uni<Void>> sendActions = connections.stream()
                .filter(c -> roomId.equals(c.pathParam("roomId")))
                .map(c -> c.sendText(payload))
                .toList();

        if (sendActions.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        return Uni.combine().all()
                .unis(sendActions)
                .discardItems();

    }

    public Uni<Void> sendToRoom(
            String roomId, String payload) {

        List<Uni<Void>> sendActions = connections.stream()
                .filter(c -> roomId.equals(c.pathParam("roomId")))
                .map(c -> c.sendText(payload))
                .toList();

        if (sendActions.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        return Uni.combine().all()
                .unis(sendActions)
                .discardItems();

    }
}