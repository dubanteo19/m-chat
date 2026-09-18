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
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchat.auth.dto.response.UserInfo;
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
    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());

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

    private void sendPushSync(PushRecipientInfo recipient, String title, String body, String roomId) {
        if (recipient.endpoint() == null)
            return;

        try {
            String url = "/room/" + roomId;
            Map<String, String> payload = Map.of("title", title, "body", body, "url", url);
            logger.info("Sending push notification to " + recipient.username() + ": " + payload);
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

    public void sendNotificationForMessage(Set<UserInfo> onlineUsers, String roomId, Long senderId,
            String senderDisplayname, String content) {

        Set<Long> onlineUserId = (onlineUsers == null)
                ? Collections.emptySet()
                : onlineUsers.stream()
                        .map(UserInfo::id)
                        .collect(Collectors.toSet());
        logger.info("Online users in room " + roomId + ": " + onlineUserId);
        long now = System.currentTimeMillis();
        Set<Long> mentionedUserIds = extractMentionedUserIds(content);

        roomService.getRoomPushRecipients(roomId)
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .subscribe().with(
                        recipients -> {
                            String title = "New message from " + senderDisplayname;

                            recipients.stream()
                                    .filter(r -> !r.userId().equals(senderId))
                                    .filter(r -> !onlineUserId.contains(r.userId()))
                                    .filter(r -> !mentionedUserIds.contains(r.userId()))
                                    .filter(r -> shouldSendNotification(roomId, r.username(), now))
                                    .forEach(r -> {
                                        sendPushSync(r, title, content, roomId);
                                        lastNotifiedMap.put(
                                                getCooldownKey(roomId, r.username()),
                                                now);
                                    });

                            sendMentionNotifications(
                                    senderId,
                                    senderDisplayname,
                                    content,
                                    roomId,
                                    recipients,
                                    onlineUserId,
                                    mentionedUserIds);
                        },
                        failure -> logger.severe(
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
            Long senderId,
            String senderDisplayname,
            String content,
            String roomId,
            List<PushRecipientInfo> recipients,
            Set<Long> onlineUserId,
            Set<Long> mentionedUserIds) {

        if (mentionedUserIds.isEmpty()) {
            return;
        }

        recipients.stream()
                .filter(r -> mentionedUserIds.contains(r.userId()))
                .filter(r -> !r.userId().equals(senderId))
                .filter(r -> !onlineUserId.contains(r.userId()))
                .forEach(r -> {
                    String title = senderDisplayname + " mentioned you";
                    String body = content;
                    sendPushSync(r, title, body, roomId);
                });
    }

    private Set<Long> extractMentionedUserIds(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptySet();
        }
        var matcher = MENTION_PATTERN.matcher(content);

        Set<Long> userIds = new HashSet<>();

        while (matcher.find()) {
            try {
                userIds.add(Long.parseLong(matcher.group(1)));
            } catch (NumberFormatException e) {
                logger.warning("Invalid user ID in mention: " + matcher.group(1));
            }
        }

        return userIds;
    }
}