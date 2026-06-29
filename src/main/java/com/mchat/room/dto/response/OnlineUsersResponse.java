package com.mchat.room.dto.response;

import java.util.Collection;

import com.mchat.auth.dto.response.UserInfo;

public record OnlineUsersResponse(
        String type, // "ONLINE_USERS"
        Collection<UserInfo> users) {
    public static OnlineUsersResponse from(Collection<UserInfo> users) {
        return new OnlineUsersResponse("ONLINE_USERS", users);
    }
}