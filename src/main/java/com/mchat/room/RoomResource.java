package com.mchat.room;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.mchat.room.dto.request.CreateRoomRequest;
import com.mchat.room.dto.request.MessagePaginationRequest;
import com.mchat.room.dto.response.MessageDeleteResponse;
import com.mchat.socket.ChatBroadcaster;

import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/rooms")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

  @Inject
  RoomService roomService;
  @Inject
  ChatBroadcaster chatBroadcaster;
  @Inject
  JsonWebToken jwt;

  @POST
  public Uni<Response> create(CreateRoomRequest request) {
    return roomService.create(request).map(payload -> Response.ok(payload).build());
  }

  @DELETE
  @Path("/{roomId}/messages/{messageId}")
  public Uni<Response> deleteMessage(@PathParam("roomId") String roomId,
      @PathParam("messageId") Long messageId) {
    String username = jwt.getName();
    return roomService.unsendMessage(messageId)
        .chain(deletedMessage -> {
          var messageDeletedResponse = MessageDeleteResponse.create(messageId, username);
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
      @PathParam("roomId") String room, @BeanParam MessagePaginationRequest pagination) {
    return roomService
        .getRoomMessagesPaginated(room, pagination)
        .map(payload -> Response.ok(payload).build());
  }
}
