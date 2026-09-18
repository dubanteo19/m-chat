package com.mchat.message.event;

import com.mchat.notification.NotificationService;
import com.mchat.socket.ChatSocket;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class PushNotificationListener {

    @Inject
    NotificationService notificationService;

    void onMessageCreated(
            @Observes MessageCreatedEvent event) {

        var onlineUsers = ChatSocket.getOnlineUsers(event.roomId());

        notificationService.sendNotificationForMessage(
                onlineUsers,
                event.roomId(),
                event.senderId(),
                event.senderDisplayname(),
                event.previewContent()
            );
    }
}