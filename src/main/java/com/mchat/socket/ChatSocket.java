package com.mchat.socket;

import org.jboss.logging.Logger;

import com.mchat.room.RoomService;
import com.mchat.room.dto.response.MessageResponse;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@WebSocket(path = "/chat/{roomId}/{username}")
public class ChatSocket {

  private static final Logger LOG = Logger.getLogger(ChatSocket.class);

  @Inject
  WebSocketConnection connection;
  @Inject
  RoomService roomService;

  @OnOpen
  public Uni<Void> onOpen() {
    String username = connection.pathParam("username");
    String roomId = connection.pathParam("roomId");
    LOG.info(username + " connected to room: " + roomId);
    return connection.broadcast()
        .filter(c -> c.pathParam("roomId").equals(roomId) && !c.equals(connection))
        .<MessageResponse>sendText(MessageResponse.createJoinMessage(username));
  }

  @OnTextMessage(broadcast = true)
  public Uni<Void> onMessage(String textContent) {
    String username = connection.pathParam("username");
    String roomId = connection.pathParam("roomId");

    return roomService
        .saveIncomingMessage(roomId, username, textContent)
        .chain(
            savedMessage -> {
              return connection
                  .broadcast()
                  .filter(c -> c.pathParam("roomId").equals(roomId))
                  .<MessageResponse>sendText(MessageResponse.from(savedMessage));
            });
  }

  @OnClose
  public void onClose() {
    String username = connection.pathParam("username");
    LOG.info(username + " left the chat.");
  }
}
