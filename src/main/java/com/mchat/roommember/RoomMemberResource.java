package com.mchat.roommember;

import org.eclipse.microprofile.jwt.Claim;

import com.mchat.roommember.dto.request.InviteMemberRequest;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/rooms/{roomId}/members")
@RequestScoped
public class RoomMemberResource {

  @Inject
  RoomMemberService roomMemberService;

  @Inject
  @Claim("userId")
  Long currentUserId;

  @POST
  public Uni<Response> inviteUser(@PathParam("roomId") String roomId, InviteMemberRequest request) {
    return roomMemberService
        .inviteUser(currentUserId, roomId, request.username())
        .map(member -> Response.status(Response.Status.CREATED).entity(member).build());
  }

  @DELETE
  @Path("/{username}")
  public Uni<Response> kickUser(
      @PathParam("roomId") String roomId,
      @PathParam("username") String targetUsername) {

    return roomMemberService
        .kickUser(currentUserId, roomId, targetUsername)
        .map(ignored -> Response.noContent().build());
  }
}
