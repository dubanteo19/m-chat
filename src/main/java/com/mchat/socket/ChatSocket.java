package com.mchat.socket;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchat.auth.dto.response.UserInfo;
import com.mchat.message.dto.response.MessageResponse;
import com.mchat.notification.NotificationService;
import com.mchat.room.RoomService;
import com.mchat.room.dto.response.OnlineUsersResponse;
import com.mchat.socket.dto.EventType;
import com.mchat.user.UserService;

import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@WebSocket(path = "/chat/{roomId}/{username}")
public class ChatSocket {

  private static final ConcurrentHashMap<String, Set<UserInfo>> roomUsers = new ConcurrentHashMap<>();

  @Inject
  NotificationService notificationService;
  @Inject
  WebSocketConnection connection;
  @Inject
  RoomService roomService;
  @Inject
  ObjectMapper objectMapper;
  @Inject
  UserService userService;
  @Inject
  ChatBroadcaster chatBroadcaster;

  public static Set<UserInfo> getOnlineUsers(String roomId) {
    return roomUsers.getOrDefault(roomId, Collections.emptySet());
  }

  public static boolean isOnlineInRoom(String roomId, String username) {
    Set<UserInfo> users = roomUsers.get(roomId);
    if (users == null)
      return false;

    return users.stream().anyMatch(u -> u.username().equals(username));
  }

  @OnOpen
  public Uni<Void> onOpen() {
    String username = connection.pathParam("username");
    String roomId = connection.pathParam("roomId");
    Log.infof("%s connecting to room: %s", username, roomId);

    return userService
        .findByUsername(username)
        .chain(
            user -> {
              if (user == null) {
                return connection.close();
              }

              var userInfo = UserInfo.fromEntity(user);
              roomUsers.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(userInfo);
              var joinMessage = MessageResponse.createSystemMessage(userInfo.displayName() + " joined the room.");
              var onlineUsersPayload = OnlineUsersResponse.from(roomUsers.get(roomId));

              try {
                String joinJson = objectMapper.writeValueAsString(joinMessage);
                String onlineJson = objectMapper.writeValueAsString(onlineUsersPayload);

                Uni<Void> broadcastJoinAlert = chatBroadcaster.sendToRoom(roomId, joinJson);
                Uni<Void> broadcastUserList = chatBroadcaster.sendToRoom(roomId, onlineJson);

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
  public Uni<Void> onMessage(String payload) {
    String username = connection.pathParam("username");
    String roomId = connection.pathParam("roomId");

    try {
      var jsonNode = objectMapper.readTree(payload);
      var eventType = EventType.valueOf(jsonNode.get("eventType").asText().toUpperCase());
      if (EventType.PING.equals(eventType)) {
        return Uni.createFrom().voidItem();
      }

      // --- FORWARDABLE EVENTS ---
      Log.infof("Forwarding event %s from user %s in room %s", eventType, username, roomId);
      return chatBroadcaster.sendToRoom(roomId, payload);

    } catch (Exception e) {

      return chatBroadcaster.sendToRoom(roomId, payload);
    }
  }

  @OnClose
  public Uni<Void> onClose() {
    String username = connection.pathParam("username");
    String roomId = connection.pathParam("roomId");
    Log.infof("%s left room: %s", username, roomId);

    if (roomId == null || username == null) {
      return Uni.createFrom().voidItem();
    }

    Set<UserInfo> users = roomUsers.get(roomId);
    if (users != null) {
      var leavingUser = users.stream().filter(u -> username.equals(u.username())).findFirst().orElse(null);

      users.removeIf(u -> username.equals(u.username()));

      if (users.isEmpty()) {
        roomUsers.remove(roomId);
        return Uni.createFrom().voidItem();
      }

      var leaveMessage = MessageResponse.createSystemMessage(leavingUser.displayName() + " left the room.");
      var updatedOnlineUsersPayload = OnlineUsersResponse.from(users);

      try {
        String leaveJson = objectMapper.writeValueAsString(leaveMessage);
        String onlineJson = objectMapper.writeValueAsString(updatedOnlineUsersPayload);

        Uni<Void> broadcastLeaveAlert = chatBroadcaster.sendToRoom(roomId, leaveJson);

        Uni<Void> broadcastUpdatedList = chatBroadcaster.sendToRoom(roomId, onlineJson);

        return Uni.combine().all().unis(broadcastLeaveAlert, broadcastUpdatedList).discardItems();

      } catch (JsonProcessingException e) {
        Log.error("Failed to serialize leave/user-list payloads", e);
        return Uni.createFrom().failure(e);
      }
    }

    return Uni.createFrom().voidItem();
  }
}
