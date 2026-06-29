package com.mchat.socket;

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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.jboss.logging.Logger;

@WebSocket(path = "/chat/{roomId}/{username}")
public class ChatSocket {

  private static final Logger LOG = Logger.getLogger(ChatSocket.class);
  private static final ConcurrentHashMap<String, Set<UserInfo>> roomUsers =
      new ConcurrentHashMap<>();
  @Inject WebSocketConnection connection;
  @Inject RoomService roomService;
  @Inject ObjectMapper objectMapper;
  @Inject UserService userService;

  @OnOpen
  public Uni<Void> onOpen() {
    String username = connection.pathParam("username");
    String roomId = connection.pathParam("roomId");
    LOG.info(username + " connecting to room: " + roomId);

    return userService
        .findByUsername(username)
        .chain(
            user -> {
              if (user == null) {
                return connection.close(); // Force close connection if user entity doesn't exist
              }

              var userInfo = UserInfo.fromEntity(user);
              roomUsers.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(userInfo);
              var joinMessage =
                  MessageResponse.createSystemMessage(userInfo.displayName() + " joined the room.");
              var onlineUsersPayload = OnlineUsersResponse.from(roomUsers.get(roomId));

              try {
                String joinJson = objectMapper.writeValueAsString(joinMessage);
                String onlineJson = objectMapper.writeValueAsString(onlineUsersPayload);

                // Broadcast the text "Join" alert to EVERYONE ELSE in that specific room
                Uni<Void> broadcastJoinAlert =
                    connection
                        .broadcast()
                        .filter(c -> roomId.equals(c.pathParam("roomId")) && !c.equals(connection))
                        .sendText(joinJson);

                // Broadcast the FRESH USER LIST to EVERYONE in that specific room (including the
                // new user)
                Uni<Void> broadcastUserList =
                    connection
                        .broadcast()
                        .filter(c -> roomId.equals(c.pathParam("roomId")))
                        .sendText(onlineJson);

                return Uni.combine()
                    .all()
                    .unis(broadcastUserList, broadcastJoinAlert)
                    .discardItems();

              } catch (JsonProcessingException e) {
                return Uni.createFrom().failure(e);
              }
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
                  var responsePayload =
                      MessageUpdateResponse.createReactionUpdate(
                          messageId, emoji, result.reaction(), result.user());

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
      Long parentId =
          jsonNode.has("replyTo") && !jsonNode.get("replyTo").isNull()
              ? jsonNode.get("replyTo").asLong()
              : null;
      return roomService
          .saveIncomingMessage(roomId, username, finalContent, messageType, parentId)
          .chain(
              savedMessage ->
                  connection
                      .broadcast()
                      .filter(c -> c.pathParam("roomId").equals(roomId))
                      .<MessageResponse>sendText(MessageResponse.from(savedMessage)));

    } catch (Exception e) {

      return roomService
          .saveIncomingMessage(roomId, username, textContent, MessageType.TEXT, null)
          .chain(
              savedMessage ->
                  connection
                      .broadcast()
                      .filter(c -> c.pathParam("roomId").equals(roomId))
                      .<MessageResponse>sendText(MessageResponse.from(savedMessage)));
    }
  }

  @OnClose
  public Uni<Void> onClose() {
    String username = connection.pathParam("username");
    String roomId = connection.pathParam("roomId");
    LOG.info(username + " left room: " + roomId);

    if (roomId == null || username == null) {
      return Uni.createFrom().voidItem();
    }

    Set<UserInfo> users = roomUsers.get(roomId);
    if (users != null) {
      var leavingUser =
          users.stream().filter(u -> username.equals(u.username())).findFirst().orElse(null);

      users.removeIf(u -> username.equals(u.username()));

      if (users.isEmpty()) {
        roomUsers.remove(roomId);
        return Uni.createFrom().voidItem();
      }

      var leaveMessage =
          MessageResponse.createSystemMessage(leavingUser.displayName() + " left the room.");
      var updatedOnlineUsersPayload = OnlineUsersResponse.from(users);

      try {
        String leaveJson = objectMapper.writeValueAsString(leaveMessage);
        String onlineJson = objectMapper.writeValueAsString(updatedOnlineUsersPayload);

        Uni<Void> broadcastLeaveAlert =
            connection
                .broadcast()
                .filter(c -> roomId.equals(c.pathParam("roomId")))
                .sendText(leaveJson);

        Uni<Void> broadcastUpdatedList =
            connection
                .broadcast()
                .filter(c -> roomId.equals(c.pathParam("roomId")))
                .sendText(onlineJson);

        return Uni.combine().all().unis(broadcastLeaveAlert, broadcastUpdatedList).discardItems();

      } catch (JsonProcessingException e) {
        LOG.error("Failed to serialize leave/user-list payloads", e);
        return Uni.createFrom().failure(e);
      }
    }

    return Uni.createFrom().voidItem();
  }
}
