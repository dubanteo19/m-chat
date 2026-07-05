package com.mchat.roommember;

import com.mchat.roommember.dto.request.InviteMemberRequest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/rooms/{roomId}/members")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RoomMemberResource {

  @Inject RoomMemberService roomMemberService;

  @POST
  public Uni<Response> inviteUser(@PathParam("roomId") String roomId, InviteMemberRequest request) {
    return roomMemberService
        .inviteUser(roomId, request.username())
        .map(member -> Response.status(Response.Status.CREATED).entity(member).build());
  }

  @DELETE
  @Path("/{username}")
  public Uni<Response> kickUser(
      @PathParam("roomId") String roomId,
      @PathParam("username") String targetUsername,
      @HeaderParam("X-Username") String actorUsername) {

    return roomMemberService
        .kickUser(roomId, targetUsername, actorUsername)
        .map(ignored -> Response.noContent().build());
  }
}
