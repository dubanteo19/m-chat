
package com.mchat.message;

import org.eclipse.microprofile.jwt.Claim;

import com.mchat.message.dto.request.MessageCreateRequest;
import com.mchat.message.dto.request.MessagePaginationRequest;
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

@Path("/rooms/{roomId}/messages")
@RequestScoped
public class MessageResource {

        @Inject
        MessageService messageService;
        @Inject
        ChatBroadcaster chatBroadcaster;
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
                                .chain(deletedMessageResponse -> chatBroadcaster.sendToRoom(roomId,
                                                deletedMessageResponse))
                                .map(payload -> Response.ok(payload).build());
        }

        @POST
        public Uni<Response> sendMessage(
                        @PathParam("roomId") String roomId,
                        MessageCreateRequest request) {

                return messageService
                                .saveIncomingMessage(
                                                currentUserId,
                                                roomId,
                                                request.content(),
                                                request.type(),
                                                request.parentId())
                                .chain(messageResponse -> chatBroadcaster.sendToRoom(roomId, messageResponse)
                                                .replaceWith(Response.ok(messageResponse).build()));
        }

}