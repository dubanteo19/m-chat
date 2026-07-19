package com.mchat.roommember.dto.response;

import com.mchat.auth.dto.response.UserInfo;
import com.mchat.model.RoomMember;
import com.mchat.model.RoomRole;
import java.time.Instant;

public record RoomMemberInfo(UserInfo user, RoomRole role, Instant joinedAt) {
  public static RoomMemberInfo fromEntity(RoomMember roomMember) {
    return new RoomMemberInfo(
        UserInfo.fromEntity(roomMember.user), roomMember.role, roomMember.joinedAt);
  }
}
