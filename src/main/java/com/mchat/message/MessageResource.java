
package com.mchat.message;

import org.eclipse.microprofile.jwt.Claim;

import com.mchat.room.dto.request.MessagePaginationRequest;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/rooms/{roomId}/messages")
@RequestScoped
public class MessageResource {

    @Inject
    MessageService messageService;

    @Inject
    @Claim("userId")
    Long currentUserId;

    @GET
    public Uni<Response> getMessages(
            @PathParam("roomId") String roomId,
            @BeanParam MessagePaginationRequest pagination) {

        return messageService
                .getRoomMessagesPaginated(roomId, pagination)
                .map(payload -> Response.ok(payload).build());
    }

    @DELETE
    @Path("/{messageId}")
    public Uni<Response> deleteMessage(
            @PathParam("roomId") String roomId,
            @PathParam("messageId") Long messageId) {

        return messageService
                .unsendMessage(currentUserId, messageId)
                .map(payload -> Response.ok(payload).build());
    }
}