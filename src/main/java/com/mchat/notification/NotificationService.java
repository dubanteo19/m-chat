package com.mchat.notification;

import java.security.Security;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchat.auth.dto.response.UserInfo;
import com.mchat.model.Message;
import com.mchat.notification.dto.response.PushRecipientInfo;
import com.mchat.room.RoomService;

import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;

@ApplicationScoped
public class NotificationService {

    @Inject
    Executor executor;
    private final PushService pushService;
    @Inject
    ObjectMapper objectMapper;

    @Inject
    RoomService roomService;
    private final Map<String, Long> lastNotifiedMap = new ConcurrentHashMap<>();
    private static final Pattern MENTION_PATTERN = Pattern.compile("<@([a-zA-Z0-9_-]+)>");

    private static final long COOLDOWN_MS = Duration.ofMinutes(2).toMillis();

    public NotificationService(
            @ConfigProperty(name = "vapid.public.key") String publicKey,
            @ConfigProperty(name = "vapid.private.key") String privateKey,
            @ConfigProperty(name = "vapid.subject") String subject)
            throws Exception {

        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        this.pushService = new PushService(publicKey, privateKey, subject);
    }

    private void sendPushSync(PushRecipientInfo recipient, String title, String body) {
        if (recipient.endpoint() == null)
            return;

        try {
            Map<String, String> payload = Map.of("title", title, "body", body);
            System.out.println("Sending push notification to " + recipient.username() + ": " + payload);
            String json = objectMapper.writeValueAsString(payload);

            var notification = new Notification(
                    recipient.endpoint(),
                    recipient.p256dh(),
                    recipient.auth(),
                    json);
            pushService.send(notification, Encoding.AES128GCM);
        } catch (Exception e) {
            System.err.println("Error sending push to " + recipient.username() + ": " + e.getMessage());
        }
    }

    public void sendNotificationForMessage(Message savedMessage, String roomId, Set<UserInfo> onlineUsers) {

        Set<String> onlineUsernames = (onlineUsers == null)
                ? Collections.emptySet()
                : onlineUsers.stream()
                        .map(UserInfo::username)
                        .collect(Collectors.toSet());
        System.out.println("Online users in room " + roomId + ": " + onlineUsernames);
        String senderUsername = savedMessage.sender.username;
        long now = System.currentTimeMillis();
        Set<String> mentionedUserIds = extractMentionedUserIds(savedMessage.content);

        roomService.getRoomPushRecipients(roomId)
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .subscribe().with(
                        recipients -> {
                            String title = "New message from " + senderUsername;
                            String body = savedMessage.content;

                            recipients.stream()
                                    .filter(r -> !r.username().equals(senderUsername))
                                    .filter(r -> !onlineUsernames.contains(r.username()))
                                    .filter(r -> !mentionedUserIds.contains(String.valueOf(r.userId())))
                                    .filter(r -> shouldSendNotification(roomId, r.username(), now))
                                    .forEach(r -> {
                                        sendPushSync(r, title, body);
                                        lastNotifiedMap.put(
                                                getCooldownKey(roomId, r.username()),
                                                now);
                                    });

                            sendMentionNotifications(
                                    savedMessage,
                                    roomId,
                                    recipients,
                                    onlineUsernames,
                                    mentionedUserIds);
                        },
                        failure -> System.err.println(
                                "Failed to process notifications: " + failure.getMessage()));
    }

    private boolean shouldSendNotification(String roomId, String username, long now) {
        String key = getCooldownKey(roomId, username);
        Long lastSent = lastNotifiedMap.get(key);

        return lastSent == null || (now - lastSent) > COOLDOWN_MS;
    }

    private String getCooldownKey(String roomId, String username) {
        return roomId + ":" + username;
    }

    private void sendMentionNotifications(
            Message savedMessage,
            String roomId,
            List<PushRecipientInfo> recipients,
            Set<String> onlineUsernames,
            Set<String> mentionedUserIds) {

        if (mentionedUserIds.isEmpty()) {
            return;
        }

        String senderUsername = savedMessage.sender.username;

        recipients.stream()
                .filter(r -> mentionedUserIds.contains(String.valueOf(r.userId())))
                .filter(r -> !r.username().equals(senderUsername))
                .filter(r -> !onlineUsernames.contains(r.username()))
                .forEach(r -> {
                    String title = savedMessage.sender.displayName
                            + " mentioned you";

                    String body = savedMessage.content;

                    sendPushSync(r, title, body);
                });
    }

    private Set<String> extractMentionedUserIds(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptySet();
        }
        var matcher = MENTION_PATTERN.matcher(content);

        Set<String> userIds = new HashSet<>();

        while (matcher.find()) {
            userIds.add(matcher.group(1));
        }

        return userIds;
    }
}