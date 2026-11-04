package com.mchat.sticker;

import com.mchat.event.StickerProcessingJob;
import com.mchat.socket.UserEventBroadcaster;
import com.mchat.sticker.dto.request.CreateStickerPackRequest;
import com.mchat.sticker.dto.request.CreateStickerRequest;
import com.mchat.sticker.dto.request.UpdateStickerPackRequest;
import com.mchat.user.dto.event.StickerStatus;
import com.mchat.user.dto.event.StickerStatusChangedEvent;
import com.mchat.user.dto.event.UserEvent;
import com.mchat.user.dto.event.UserEventType;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.reactive.messaging.Channel;

@Path("/sticker-packages")
@RequestScoped
public class StickerPackageResource {

    @Inject
    @Claim("userId")
    Long currentUserId;

    @Inject
    StickerPackageService stickerPackageService;

    @Inject
    StickerService stickerService;

    @Inject
    UserEventBroadcaster userEventBroadCaster;

    @Inject
    @Channel("sticker-jobs-out")
    MutinyEmitter<StickerProcessingJob> jobEmitter;

    @POST
    public Uni<Response> createPack(@Valid CreateStickerPackRequest request) {
        return stickerPackageService
                .create(currentUserId, request)
                .map(packageId -> Response.status(Response.Status.CREATED).entity(packageId).build());
    }

    @PUT
    public Uni<Response> updatePack(@Valid UpdateStickerPackRequest request) {
        return stickerPackageService
                .update(currentUserId, request)
                .map(packageId -> Response.status(Response.Status.CREATED).entity(packageId).build());
    }

    @GET
    @Path("/my")
    public Uni<Response> getMyPackages() {
        return stickerPackageService
                .getPackages(currentUserId)
                .map(packages -> Response.ok().entity(packages).build());
    }

    @GET
    public Uni<Response> getPublicPackages() {
        return stickerPackageService
                .getPublicPackages()
                .map(packages -> Response.ok().entity(packages).build());
    }

    @POST
    @Path("/{packageId}/stickers")
    public Uni<Response> addSticker(
            @PathParam("packageId") Long packageId, CreateStickerRequest request) {
        return stickerPackageService
                .addSticker(currentUserId, packageId, request)
                .chain(
                        stickerId -> {
                            var job = new StickerProcessingJob(
                                    stickerId, packageId, request.objectKey(), currentUserId);
                            return jobEmitter
                                    .send(job)
                                    .invoke(
                                            () -> userEventBroadCaster.send(
                                                    currentUserId,
                                                    new UserEvent(
                                                            UserEventType.STICKER_STATUS_CHANGED,
                                                            new StickerStatusChangedEvent(stickerId,
                                                                    StickerStatus.QUEUED))))
                                    .replaceWith(stickerId);
                        })
                .map(stickerId -> Response.status(Response.Status.CREATED).entity(stickerId).build());
    }

    @DELETE
    @Path("/{packageId}")
    public Uni<Response> deletePack(@PathParam("packageId") Long packageId) {
        return stickerPackageService
                .deletePack(currentUserId, packageId)
                .map(removedPackageId -> Response.status(Response.Status.NO_CONTENT).build());
    }

    @DELETE
    @Path("/{packageId}/stickers/{stickerId}")
    public Uni<Response> removeSticker(
            @PathParam("packageId") Long packageId, @PathParam("stickerId") Long stickerId) {
        return stickerPackageService
                .removeSticker(currentUserId, packageId, stickerId)
                .map(removedStickerId -> Response.status(Response.Status.NO_CONTENT).build());
    }

    @POST
    @Path("/{packageId}/download")
    public Uni<Response> donwloadPack(@PathParam("packageId") Long packageId) {
        return stickerPackageService
                .download(packageId)
                .map(presignUrl -> Response.ok().entity(presignUrl).build());
    }
}
