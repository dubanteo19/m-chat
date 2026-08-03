package com.mchat.user.dto.request;

import com.mchat.model.json.TitleStyle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
  @NotBlank(message = "Display name is required")
  @Size(min = 2, max = 50, message = "Display name must be between 2 and 50 characters")
  public String displayName;
  @Size(max = 30, message = "Title must not exceed 30 characters")
  public String title;
  public String avatarUrl;
  public TitleStyle titleStyle;
}
