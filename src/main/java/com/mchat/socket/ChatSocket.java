package com.mchat.socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchat.auth.dto.response.UserInfo;
import com.mchat.model.MessageType;
import com.mchat.notification.NotificationService;
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

  @Inject NotificationService notificationService;
  @Inject WebSocketConnection connection;
  @Inject RoomService roomService;
  @Inject ObjectMapper objectMapper;
  @Inject UserService userService;

  public static <M> Uni<Void> sendToRoom(WebSocketConnection connection, String roomId, M payload) {
    return connection
        .broadcast()
        .filter(c -> roomId.equals(c.pathParam("roomId")))
        .sendText(payload);
  }

  public static Uni<Void> sendToRoom(
      WebSocketConnection connection, String roomId, String payload) {
    return connection
        .broadcast()
        .filter(c -> roomId.equals(c.pathParam("roomId")))
        .sendText(payload);
  }

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

                Uni<Void> broadcastJoinAlert = sendToRoom(connection, roomId, joinJson);
                Uni<Void> broadcastUserList = sendToRoom(connection, roomId, onlineJson);

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

      // --- TYPING STATUS INDICATORS ---
      if ("PING".equals(typeStr)) {
        return Uni.createFrom().voidItem();
      }
      // --- TYPING STATUS INDICATORS ---
      if ("TYPING_START".equals(typeStr) || "TYPING_STOP".equals(typeStr)) {
        return sendToRoom(connection, roomId, textContent);
      }
      // --- CASE MESSAGE REACTIONS ---
      if ("REACTION".equals(typeStr)) {
        Long messageId = jsonNode.get("messageId").asLong();
        String emoji = jsonNode.get("content").asText();

        return roomService
            .saveReaction(roomId, username, messageId, emoji)
            .chain(
                result -> {
                  var responsePayload =
                      MessageUpdateResponse.createReactionUpdate(
                          messageId, emoji, result.reaction(), result.user());

                  try {
                    String jsonText = objectMapper.writeValueAsString(responsePayload);
                    return sendToRoom(connection, roomId, jsonText);
                  } catch (JsonProcessingException e) {
                    return Uni.createFrom().failure(e);
                  }
                });
      }

      // --- CHAT MESSAGES & INLINE REPLIES ---
      var messageType = MessageType.valueOf(typeStr.toUpperCase());
      String finalContent = jsonNode.get("content").asText();
      Long parentId =
          jsonNode.has("replyTo") && !jsonNode.get("replyTo").isNull()
              ? jsonNode.get("replyTo").asLong()
              : null;
      return roomService
          .saveIncomingMessage(roomId, username, finalContent, messageType, parentId)
          .chain(
              savedMessage -> sendToRoom(connection, roomId, MessageResponse.from(savedMessage)));

    } catch (Exception e) {

      return roomService
          .saveIncomingMessage(roomId, username, textContent, MessageType.TEXT, null)
          .chain(
              savedMessage -> sendToRoom(connection, roomId, MessageResponse.from(savedMessage)));
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

        Uni<Void> broadcastLeaveAlert = sendToRoom(connection, roomId, leaveJson);

        Uni<Void> broadcastUpdatedList = sendToRoom(connection, roomId, onlineJson);

        return Uni.combine().all().unis(broadcastLeaveAlert, broadcastUpdatedList).discardItems();

      } catch (JsonProcessingException e) {
        LOG.error("Failed to serialize leave/user-list payloads", e);
        return Uni.createFrom().failure(e);
      }
    }

    return Uni.createFrom().voidItem();
  }
}
