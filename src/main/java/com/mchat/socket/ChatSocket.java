package com.mchat.socket;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchat.model.MessageType;
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
  @Inject
  ObjectMapper objectMapper;

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

    try {
      var jsonNode = objectMapper.readTree(textContent);

      String typeStr = jsonNode.get("type").asText();

      // --- CASE 0: TYPING STATUS INDICATORS ---
      if ("PING".equals(typeStr)) {
        return Uni.createFrom().voidItem();
      }
      // --- CASE 1: TYPING STATUS INDICATORS ---
      if ("TYPING_START".equals(typeStr) || "TYPING_STOP".equals(typeStr)) {
        return connection.broadcast()
            .filter(c -> c.pathParam("roomId").equals(roomId))
            .sendText(textContent);
      }

      // --- CASE 1.5: MESSAGE REACTIONS ---
      if ("REACTION".equals(typeStr)) {
        Long messageId = jsonNode.get("messageId").asLong();
        String emoji = jsonNode.get("content").asText(); // content houses the emoji string e.g., '👍'

        return roomService.saveReaction(roomId, username, messageId, emoji)
            .chain(reaction -> {
              var responseNode = objectMapper.createObjectNode();
              responseNode.put("type", "REACTION");
              responseNode.put("messageId", messageId);
              responseNode.put("username", username);
              responseNode.put("content", emoji);
              responseNode.put("action", reaction == null ? "REMOVED" : "ADDED");

              return connection.broadcast()
                  .filter(c -> c.pathParam("roomId").equals(roomId))
                  .sendText(responseNode.toString());
            });
      }

      // --- CASE 2: CHAT MESSAGES & INLINE REPLIES ---
      MessageType messageType = MessageType.valueOf(typeStr.toUpperCase());
      String finalContent = jsonNode.get("content").asText();
      Long parentId = jsonNode.has("parentId") && !jsonNode.get("parentId").isNull()
          ? jsonNode.get("parentId").asLong()
          : null;
      return roomService
          .saveIncomingMessage(roomId, username, finalContent, messageType, parentId)
          .chain(savedMessage -> connection.broadcast()
              .filter(c -> c.pathParam("roomId").equals(roomId))
              .<MessageResponse>sendText(MessageResponse.from(savedMessage)));

    } catch (Exception e) {

      return roomService
          .saveIncomingMessage(roomId, username, textContent, MessageType.TEXT, null)
          .chain(savedMessage -> connection.broadcast()
              .filter(c -> c.pathParam("roomId").equals(roomId))
              .<MessageResponse>sendText(MessageResponse.from(savedMessage)));
    }
  }

  @OnClose
  public void onClose() {
    String username = connection.pathParam("username");
    LOG.info(username + " left the chat.");
  }
}
