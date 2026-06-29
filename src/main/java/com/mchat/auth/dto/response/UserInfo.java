package com.mchat.auth.dto.response;

import com.mchat.model.User;

public record UserInfo(Long id, String username, String displayName, String avatarUrl, String title) {
  public UserInfo(String username, String displayName, String avatarUrl, String title) {
    this(999999999L, username, displayName, avatarUrl, title);
  }
  public static UserInfo fromEntity(User user) {
    return new UserInfo(user.id, user.username, user.displayName, user.avatarUrl, user.title);
  }
}
