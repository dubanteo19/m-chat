package com.mchat.user;

import com.mchat.model.json.PushSubscription;
import com.mchat.notification.NotificationService;
import com.mchat.roommember.RoomMemberService;
import com.mchat.user.dto.request.UpdateProfileRequest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

  private static final Logger LOG = Logger.getLogger(UserResource.class);
  @Inject UserService userService;
  @Inject NotificationService notificationService;
  @Inject RoomMemberService roomMemberService;

  @GET
  @Path("/{username}/profile")
  public Uni<Response> getUserProfile(@PathParam("username") String username) {
    return userService
        .getUserInfoByUsername(username)
        .onItem()
        .transform(userInfo -> Response.ok(userInfo).build());
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

  @GET
  @Path("/{username}/rooms")
  public Uni<Response> getRooms(@PathParam("username") String username) {
    return roomMemberService.findRoomsByUsername(username).map(rooms -> Response.ok(rooms).build());
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
