package com.mchat.room;

import com.mchat.room.dto.request.MessagePaginationRequest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/rooms")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

  @Inject
  RoomService roomService;

  @GET
  @Path("/{roomId}/messages")
  public Uni<Response> getRoomMessages(
      @PathParam("roomId") String room, @BeanParam MessagePaginationRequest pagination) {
    return roomService
        .getRoomMessagesPaginated(room, pagination)
        .map(payload -> Response.ok(payload).build());
  }
}
