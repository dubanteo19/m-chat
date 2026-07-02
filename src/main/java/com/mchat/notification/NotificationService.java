package com.mchat.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchat.model.User;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.security.Security;
import java.util.Map;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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
        .item(
            () -> {
              try {
                Subscription sub =
                    new Subscription(
                        recipient.pushSubscription.endpoint,
                        new Subscription.Keys(
                            recipient.pushSubscription.keys.get("p256dh"),
                            recipient.pushSubscription.keys.get("auth")));

                Map<String, String> payload =
                    Map.of(
                        "title", title,
                        "body", body);

                String json = objectMapper.writeValueAsString(payload);

                Notification notification = new Notification(sub, json);
                pushService.send(notification);
              } catch (Exception e) {
                // Log exception (e.g., subscription expired or revoked by device)
              }
              return null;
            })
        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
        .replaceWithVoid();
  }
}
