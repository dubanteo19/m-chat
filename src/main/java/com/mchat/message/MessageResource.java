package com.mchat.message;

import com.mchat.message.dto.request.MessageCreateRequest;
import com.mchat.message.dto.request.MessagePaginationRequest;
import com.mchat.message.dto.request.MessageReactRequest;
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
import org.eclipse.microprofile.jwt.Claim;

@Path("/rooms/{roomId}/messages")
@RequestScoped
public class MessageResource {

  @Inject MessageService messageService;
  @Inject ChatBroadcaster chatBroadcaster;

  @Inject
  @Claim("userId")
  Long currentUserId;

  @GET
  public Uni<Response> getMessages(
      @PathParam("roomId") String roomId, @BeanParam MessagePaginationRequest pagination) {

    return messageService
        .getRoomMessagesPaginated(roomId, pagination.getLimit(), pagination.before())
        .map(payload -> Response.ok(payload).build());
  }

  @DELETE
  @Path("/{messageId}")
  public Uni<Response> deleteMessage(
      @PathParam("roomId") String roomId, @PathParam("messageId") Long messageId) {

    return messageService
        .unsendMessage(currentUserId, messageId)
        .chain(deletedMessageResponse -> chatBroadcaster.sendToRoom(roomId, deletedMessageResponse))
        .map(payload -> Response.ok(payload).build());
  }

  @POST
  public Uni<Response> sendMessage(
      @PathParam("roomId") String roomId, MessageCreateRequest request) {
    System.out.println("request: " + request);
    return messageService
        .saveIncomingMessage(
            currentUserId, roomId, request.content(), request.type(), request.replyTo())
        .chain(
            messageResponse ->
                chatBroadcaster
                    .sendToRoom(roomId, messageResponse)
                    .replaceWith(Response.ok(messageResponse).build()));
  }

  @POST
  @Path("/{messageId}/reactions")
  public Uni<Response> reactMessage(
      @PathParam("roomId") String roomId,
      @PathParam("messageId") Long messageId,
      MessageReactRequest request) {
    return messageService
        .saveReaction(currentUserId, roomId, messageId, request.emoji())
        .chain(
            reactionResponse ->
                chatBroadcaster
                    .sendToRoom(roomId, reactionResponse)
                    .replaceWith(Response.ok(reactionResponse).build()));
  }
}
