package com.mchat.auth.dto.response;

import com.mchat.model.User;
import com.mchat.model.json.TitleStyle;
import com.mchat.storage.StorageUtil;

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
    String avatarUrl = StorageUtil.getUrlPrefix() + user.avatarUrl;

    return new CurrentUserInfo(
        user.id,
        user.username,
        user.displayName,
        avatarUrl,
        user.title,
        user.titleStyle != null ? user.titleStyle : new TitleStyle(),
        user.allowNotify);

  }
}
