package com.mchat.socket;

import com.mchat.room.RoomService;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@WebSocket(path = "/chat/{roomId}/{username}")
public class ChatSocket {

  private static final Logger LOG = Logger.getLogger(ChatSocket.class);

  @Inject WebSocketConnection connection;
  @Inject RoomService roomService;

  @OnOpen
  public void onOpen() {
    String username = connection.pathParam("username");
    String roomId = connection.pathParam("roomId");
    LOG.info(username + " connected to room: " + roomId);

    connection.broadcast().sendText(username + " joined the room.");
  }

  @OnTextMessage(broadcast = true)
  public Uni<Void> onMessage(String textContent) {
    String username = connection.pathParam("username");
    String roomId = connection.pathParam("roomId");

    return roomService
        .saveIncomingMessage(roomId, username, textContent)
        .chain(
            savedMessage -> {
              String formattedPayload = savedMessage.sender + ": " + savedMessage.content;
              return connection
                  .broadcast()
                  .filter(c -> c.pathParam("roomId").equals(roomId))
                  .sendText(formattedPayload);
            });
  }

  @OnClose
  public void onClose() {
    String username = connection.pathParam("username");
    LOG.info(username + " left the chat.");
  }
}
