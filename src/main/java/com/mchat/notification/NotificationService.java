package com.mchat.notification;

import java.security.Security;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchat.model.User;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
@ApplicationScoped
public class NotificationService {

  private final PushService pushService;
  @Inject ObjectMapper objectMapper;
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

public Uni<Void> sendPushNotification(User recipient, String title, String body) {
    if (recipient.pushSubscription == null || recipient.pushSubscription.endpoint == null) {
        return Uni.createFrom().voidItem();
    }

    return Uni.createFrom()
        .item(() -> {
            try {

                String endpoint = recipient.pushSubscription.endpoint;
                String p256dh = recipient.pushSubscription.keys.get("p256dh");
                String auth = recipient.pushSubscription.keys.get("auth");

                Map<String, String> payload = Map.of(
                    "title", title,
                    "body", body
                );
                String json = objectMapper.writeValueAsString(payload);

                var notification = new Notification(endpoint, p256dh, auth, json);

                pushService.send(notification, Encoding.AES128GCM);
               
                
            } catch (Exception e) {
                System.out.println("Error sending push notification: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        })
        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
        .replaceWithVoid();
}
}
