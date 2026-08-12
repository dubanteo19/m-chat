package com.mchat.user;

import org.eclipse.microprofile.jwt.Claim;

import com.mchat.model.json.PushSubscription;
import com.mchat.notification.NotificationService;
import com.mchat.room.RoomService;
import com.mchat.user.dto.request.UpdateProfileRequest;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/users")
@RequestScoped
public class UserResource {

  @Inject
  UserService userService;
  @Inject
  NotificationService notificationService;
  @Inject
  RoomService roomService;

  @Inject
  @Claim("userId")
  Long currentUserId;

  @GET
  public Uni<Response> searchUsers(
      @QueryParam("q") String displayName) {
    return userService
        .searchUsersByDisplayName(displayName)
        .onItem()
        .transform(users -> Response.ok(users).build());
  }

  @GET
  @Path("/{username}/profile")
  public Uni<Response> getUserProfile(@PathParam("username") String username) {
    return userService
        .getUserInfoByUsername(username)
        .onItem()
        .transform(userInfo -> Response.ok(userInfo).build());
  }

  @PUT
  @Path("/profile")
  public Uni<Response> updateProfile(@Valid UpdateProfileRequest request) {
    return userService
        .updateProfile(currentUserId, request)
        .onItem()
        .transform(updatedUser -> Response.ok(updatedUser).build());
  }

  @GET
  @Path("/rooms")
  public Uni<Response> getRooms() {
    return roomService.findMyRooms(currentUserId).map(rooms -> Response.ok(rooms).build());
  }

  @PUT
  @Path("/push-subscription")
  public Uni<Response> saveSubscription(PushSubscription subscription) {
    return userService
        .saveSubscription(currentUserId, subscription)
        .replaceWith(Response.noContent().build());
  }
}
