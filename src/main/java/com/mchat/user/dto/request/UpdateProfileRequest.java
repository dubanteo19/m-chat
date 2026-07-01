package com.mchat.user.dto.request;

import com.mchat.model.User;

public class UpdateProfileRequest {
    public String displayName;
    public String title;
    public String avatarUrl;
    public User.TitleStyle titleStyle;
}