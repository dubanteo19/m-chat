package com.mchat.room.dto.response;

import com.mchat.auth.dto.response.UserInfo;
import java.util.Collection;

public record OnlineUsersResponse(String type, Collection<UserInfo> users) {
  public static OnlineUsersResponse from(Collection<UserInfo> users) {
    return new OnlineUsersResponse("ONLINE_USERS", users);
  }
}
