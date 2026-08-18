package com.mchat.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record ToggleNotificationsRequest(
        @NotNull(message = "allowNotify status is required") Boolean allowNotify) {
}