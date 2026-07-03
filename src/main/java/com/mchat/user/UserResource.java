package com.mchat.user;

import java.util.Map;

import org.jboss.logging.Logger;

import com.mchat.model.json.PushSubscription;
import com.mchat.notification.NotificationService;
import com.mchat.user.dto.request.UpdateProfileRequest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

  private static final Logger LOG = Logger.getLogger(UserResource.class);
  @Inject UserService userService;
  @Inject NotificationService notificationService;

  @GET
  @Path("/{username}/profile")
  public Uni<Response> getUserProfile(@PathParam("username") String username) {
    return userService
        .getUserInfoByUsername(username)
        .onItem()
        .transform(userInfo -> Response.ok(userInfo).build());
  }
@POST
@Path("/{username}/test-push")
public Uni<Response> triggerTestPush(@PathParam("username") String username) {
    LOG.info("sending notification to user: " + username);
    return userService.findByUsername(username)
        .chain(user -> {
            if (user == null) {
                return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
            }
              
            // RETURN this Uni chain so Mutiny can subscribe and execute it!
            return notificationService.sendPushNotification(
                user, 
                "Test Message", 
                "Hey! If you see this, your Web Push setup is working perfectly 🎉"
            )
            .replaceWith(Response.ok(Map.of("status", "Push triggered")).build());
        });
}
  @PUT
  @Path("/{username}/profile")
  public Uni<Response> updateProfile(
      @PathParam("username") String username, UpdateProfileRequest request) {
    return userService
        .updateProfile(username, request)
        .onItem()
        .transform(updatedUser -> Response.ok(updatedUser).build());
  }

  @PUT
  @Path("/{username}/push-subscription")
  public Uni<Response> saveSubscription(
      @PathParam("username") String username, PushSubscription subscription) {
    LOG.info("Saving push subscription for user: " + username);
    return userService
        .saveSubscription(username, subscription)
        .replaceWith(Response.noContent().build());
  }
}

