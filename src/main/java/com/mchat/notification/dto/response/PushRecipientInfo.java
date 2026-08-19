package com.mchat.notification.dto.response;

import com.mchat.model.User;

public record PushRecipientInfo(
        Long userId,
        String username,
        String endpoint,
        String p256dh,
        String auth) {
    public static PushRecipientInfo fromEntity(User user) {
        if (user == null || user.pushSubscription == null || user.pushSubscription.endpoint == null) {
            return null;
        }

        var keys = user.pushSubscription.keys;
        String p256dh = keys != null ? keys.get("p256dh") : null;
        String auth = keys != null ? keys.get("auth") : null;

        return new PushRecipientInfo(user.id, user.username, user.pushSubscription.endpoint, p256dh, auth);
    }
}