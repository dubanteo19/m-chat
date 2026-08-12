package com.mchat.room;

import org.eclipse.microprofile.jwt.Claim;

import com.mchat.room.dto.request.CreateRoomRequest;
import com.mchat.room.dto.request.MessagePaginationRequest;
import com.mchat.room.dto.response.MessageDeleteResponse;
import com.mchat.socket.ChatBroadcaster;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/rooms")
@RequestScoped
public class RoomResource {

  @Inject
  RoomService roomService;
  @Inject
  ChatBroadcaster chatBroadcaster;

  @Inject
  @Claim("userId")
  Long currentUserId;

  @GET
  @Path("/{roomId}")
  public Uni<Response> getRoomInfo(
      @PathParam("roomId") String roomId) {
    return roomService.getRoomInfo(currentUserId, roomId).map(payload -> Response.ok(payload).build());
  }

  @POST
  public Uni<Response> create(CreateRoomRequest request) {
    return roomService.create(currentUserId, request).map(payload -> Response.ok(payload).build());
  }

  @DELETE
  public Uni<Response> delete(@PathParam("roomId") String roomId) {
    return roomService.delete(currentUserId, roomId)
        .map(payload -> Response.status(Response.Status.NO_CONTENT).build());
  }

  @DELETE
  @Path("/{roomId}/messages/{messageId}")
  public Uni<Response> deleteMessage(@PathParam("roomId") String roomId,
      @PathParam("messageId") Long messageId) {
    return roomService.unsendMessage(currentUserId, messageId)
        .chain(deletedMessage -> {
          var messageDeletedResponse = MessageDeleteResponse.create(messageId);
          return chatBroadcaster.sendToRoom(roomId, messageDeletedResponse);
        })
        .map(payload -> Response.ok(payload).build());
  }

  @GET
  @Path("/{roomId}/members")
  public Uni<Response> getRoomMembers(@PathParam("roomId") String roomId) {
    return roomService.getRoomMembers(roomId).map(members -> Response.ok(members).build());
  }

  @GET
  @Path("/{roomId}/messages")
  public Uni<Response> getRoomMessages(
      @PathParam("roomId") String roomId, @BeanParam MessagePaginationRequest pagination) {
    return roomService
        .getRoomMessagesPaginated(roomId, pagination)
        .map(payload -> Response.ok(payload).build());
  }
}
