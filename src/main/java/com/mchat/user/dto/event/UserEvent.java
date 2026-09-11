package com.mchat.user.dto.event;

public record UserEvent(
                String type,
                Object data) {
        public static UserEvent of(String type, Object data) {
                return new UserEvent(type, data);
        }
}