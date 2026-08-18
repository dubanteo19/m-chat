package com.mchat.auth.dto.response;

import com.mchat.model.User;
import com.mchat.model.json.TitleStyle;

public record CurrentUserInfo(
    Long id,
    String username,
    String displayName,
    String avatarUrl,
    String title,
    TitleStyle titleStyle,
    boolean allowNotify) {
  
  public CurrentUserInfo(String username, String displayName, String avatarUrl, String title) {
    this(999999999L, username, displayName, avatarUrl, title, new TitleStyle(), true);
  }

  public static CurrentUserInfo fromEntity(User user) {
    return new CurrentUserInfo(
        user.id,
        user.username,
        user.displayName,
        user.avatarUrl,
        user.title,
        user.titleStyle != null ? user.titleStyle : new TitleStyle(),
        user.allowNotify);

  }
}
