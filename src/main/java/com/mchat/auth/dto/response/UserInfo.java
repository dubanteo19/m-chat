package com.mchat.auth.dto.response;

import com.mchat.model.User;

public record UserInfo(
    Long id, String username, String displayName, String avatarUrl, String title, User.TitleStyle titleStyle) {
  public UserInfo(String username, String displayName, String avatarUrl, String title) {
    this(999999999L, username, displayName, avatarUrl, title, new User.TitleStyle());
  }

  public static UserInfo fromEntity(User user) {
    return new UserInfo(
        user.id,
        user.username,
        user.displayName,
        user.avatarUrl,
        user.title,
        user.titleStyle != null ? user.titleStyle : new User.TitleStyle());
  }

}
