package com.mchat.auth.dto.response;

import com.mchat.model.User;
import com.mchat.model.json.TitleStyle;
import com.mchat.storage.StorageUtil;

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
    String avatarUrl = StorageUtil.getUrlPrefix() + user.avatarUrl;
    return new UserInfo(
        user.id,
        user.username,
        user.displayName,
        avatarUrl,
        user.title,
        user.titleStyle != null ? user.titleStyle : new TitleStyle());
  }
}
