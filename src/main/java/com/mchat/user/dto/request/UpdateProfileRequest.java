package com.mchat.user.dto.request;

import com.mchat.model.json.TitleStyle;

public class UpdateProfileRequest {
  public String displayName;
  public String title;
  public String avatarUrl;
  public TitleStyle titleStyle;
}
