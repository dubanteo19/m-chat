package com.mchat.notification;

import com.mchat.event.MessageCreatedEvent;
import com.mchat.socket.ChatSocket;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

@ApplicationScoped
public class PushNotificationWorker {

    @Inject
    NotificationService notificationService;

    void onMessageCreated(
            @ObservesAsync MessageCreatedEvent event) {
        Log.info("Notification service unavaibale");
        // var onlineUsers = ChatSocket.getOnlineUsers(event.roomId());

        // notificationService.sendNotificationForMessage(
        // onlineUsers,
        // event.roomId(),
        // event.senderId(),
        // event.senderDisplayname(),
        // event.previewContent());
    }

}