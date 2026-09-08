package com.mchat.room;

import org.eclipse.microprofile.jwt.Claim;

import com.mchat.room.dto.request.CreateRoomRequest;
import com.mchat.room.dto.request.ReadRoomRequest;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/rooms")
@RequestScoped
public class RoomResource {

  @Inject
  RoomService roomService;

  @Inject
  @Claim("userId")
  Long currentUserId;

  @GET
  @Path("/{roomId}")
  public Uni<Response> getRoomInfo(
      @PathParam("roomId") String roomId) {
    return roomService.getRoomInfo(currentUserId, roomId).map(payload -> Response.ok(payload).build());
  }

  @PUT
  @Path("/{roomId}/read")
  public Uni<Response> read(
      @PathParam("roomId") String roomId, ReadRoomRequest request) {
    return roomService.read(currentUserId, roomId, request.seq()).map(payload -> Response.noContent().build());
  }

  @POST
  public Uni<Response> create(CreateRoomRequest request) {
    return roomService.create(currentUserId, request).map(payload -> Response.ok(payload).build());
  }

  @DELETE
  @Path("/{roomId}")
  public Uni<Response> delete(@PathParam("roomId") String roomId) {
    return roomService.delete(currentUserId, roomId)
        .map(payload -> Response.status(Response.Status.NO_CONTENT).build());
  }

  @GET
  @Path("/{roomId}/members")
  public Uni<Response> getRoomMembers(@PathParam("roomId") String roomId) {
    return roomService.getRoomMembers(roomId).map(members -> Response.ok(members).build());
  }

}
