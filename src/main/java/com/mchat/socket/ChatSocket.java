package com.mchat.socket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchat.auth.dto.response.UserInfo;
import com.mchat.model.MessageType;
import com.mchat.room.RoomService;
import com.mchat.room.dto.response.MessageResponse;
import com.mchat.room.dto.response.MessageUpdateResponse;
import com.mchat.room.dto.response.OnlineUsersResponse;
import com.mchat.user.UserService;

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
  private static final ConcurrentHashMap<String, Set<UserInfo>> roomUsers = new ConcurrentHashMap<>();
  @Inject
  WebSocketConnection connection;
  @Inject
  RoomService roomService;
  @Inject
  ObjectMapper objectMapper;
  @Inject
  UserService userService;

  @OnOpen
  public Uni<Void> onOpen() {
    String username = connection.pathParam("username");
    String roomId = connection.pathParam("roomId");
    LOG.info(username + " connecting to room: " + roomId);
    // 1. Look up the rich user profile directly from your DB
    return userService.findByUsername(username)
        .chain(user -> {
          // Map the DB Entity to your existing structural UI record
          var userInfo = new UserInfo(
              user.username,
              user.displayName,
              user.avatarUrl,
              user.title);

          // 2. Thread-safely push into your memory state
          roomUsers.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(userInfo);

          // 3. Setup messaging payloads
          var joinMessage = MessageResponse.createJoinMessage(username);
          var onlineUsersPayload = OnlineUsersResponse.from(roomUsers.get(roomId));

          return connection.broadcast()
              .filter(c -> c.pathParam("roomId").equals(roomId))
              .chain(broadcaster -> {
                try {
                  Uni<Void> sendJoinAlert = broadcaster.clone()
                      .filter(c -> !c.equals(connection))
                      .sendText(objectMapper.writeValueAsString(joinMessage));

                  Uni<Void> sendUserList = broadcaster.sendText(objectMapper.writeValueAsString(onlineUsersPayload));

                  return Uni.combine().all().unis(sendJoinAlert, sendUserList).discardItems();
                } catch (JsonProcessingException e) {
                  return Uni.createFrom().failure(e);
                }
              });
        });
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
        return connection
            .broadcast()
            .filter(c -> c.pathParam("roomId").equals(roomId))
            .sendText(textContent);
      }

      // --- CASE 1.5: MESSAGE REACTIONS ---
      if ("REACTION".equals(typeStr)) {
        Long messageId = jsonNode.get("messageId").asLong();
        String emoji = jsonNode.get("content").asText();

        return roomService
            .saveReaction(roomId, username, messageId, emoji)
            .chain(
                result -> {
                  // 1. Build the strongly typed payload using the factory method
                  var responsePayload = MessageUpdateResponse.createReactionUpdate(
                      messageId,
                      emoji,
                      result.reaction(),
                      result.user());

                  try {
                    String jsonText = objectMapper.writeValueAsString(responsePayload);
                    return connection
                        .broadcast()
                        .filter(c -> c.pathParam("roomId").equals(roomId))
                        .sendText(jsonText);
                  } catch (JsonProcessingException e) {
                    return Uni.createFrom().failure(e);
                  }
                });
      }

      // --- CASE 2: CHAT MESSAGES & INLINE REPLIES ---
      MessageType messageType = MessageType.valueOf(typeStr.toUpperCase());
      String finalContent = jsonNode.get("content").asText();
      Long parentId = jsonNode.has("replyTo") && !jsonNode.get("replyTo").isNull()
          ? jsonNode.get("replyTo").asLong()
          : null;
      return roomService
          .saveIncomingMessage(roomId, username, finalContent, messageType, parentId)
          .chain(
              savedMessage -> connection
                  .broadcast()
                  .filter(c -> c.pathParam("roomId").equals(roomId))
                  .<MessageResponse>sendText(MessageResponse.from(savedMessage)));

    } catch (Exception e) {

      return roomService
          .saveIncomingMessage(roomId, username, textContent, MessageType.TEXT, null)
          .chain(
              savedMessage -> connection
                  .broadcast()
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
