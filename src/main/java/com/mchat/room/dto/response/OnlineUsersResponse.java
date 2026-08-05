package com.mchat.room.dto.response;

import com.mchat.auth.dto.response.UserInfo;
import java.util.Collection;
import com.mchat.socket.dto.EventType;

public record OnlineUsersResponse(EventType eventType, Collection<UserInfo> users) {
  public static OnlineUsersResponse from(Collection<UserInfo> users) {
    return new OnlineUsersResponse(EventType.ONLINE_USERS, users);
  }
}
