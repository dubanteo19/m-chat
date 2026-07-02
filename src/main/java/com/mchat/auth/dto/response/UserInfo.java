package com.mchat.auth.dto.response;

import com.mchat.model.User;
import com.mchat.model.json.TitleStyle;

public record UserInfo(
    Long id,
    String username,
    String displayName,
    String avatarUrl,
    String title,
    TitleStyle titleStyle) {
  public UserInfo(String username, String displayName, String avatarUrl, String title) {
    this(999999999L, username, displayName, avatarUrl, title, new TitleStyle());
  }

  public static UserInfo fromEntity(User user) {
    return new UserInfo(
        user.id,
        user.username,
        user.displayName,
        user.avatarUrl,
        user.title,
        user.titleStyle != null ? user.titleStyle : new TitleStyle());
  }
}
