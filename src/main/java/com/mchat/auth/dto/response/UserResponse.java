package com.mchat.auth.dto.response;

import com.mchat.model.User;

public record UserResponse(Long id, String username, String displayName, String avatarUrl) {
  public static UserResponse fromEntity(User user) {
    return new UserResponse(user.id, user.username, user.displayName, user.avatarUrl);
  }
}
