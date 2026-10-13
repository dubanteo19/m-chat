package com.mchat.sticker;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.reactive.messaging.Channel;

import com.mchat.event.StickerProcessingJob;
import com.mchat.sticker.dto.request.CreateStickerPackageRequest;
import com.mchat.sticker.dto.request.CreateStickerRequest;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/sticker-packages")
@RequestScoped
public class StickerPackageResource {

        @Inject
        @Claim("userId")
        Long currentUserId;
        @Inject
        StickerPackageService stickerPackageService;
        @Inject
        @Channel("sticker-jobs-out")
        MutinyEmitter<StickerProcessingJob> jobEmitter;

        @POST
        public Uni<Response> createPackage(CreateStickerPackageRequest request) {
                return stickerPackageService.create(currentUserId, request)
                                .map(packageId -> Response.status(Response.Status.CREATED).entity(packageId).build());
        }

        @GET
        @Path("/my")
        public Uni<Response> getMyPackages() {
                return stickerPackageService.getPackages(currentUserId)
                                .map(packages -> Response.ok().entity(packages).build());
        }

        @POST
        @Path("/{packageId}/stickers")
        public Uni<Response> addSticker(
                        @PathParam("packageId") Long packageId, CreateStickerRequest request) {
                return stickerPackageService
                                .addSticker(currentUserId, packageId, request)
                                .chain(stickerId -> {
                                        var job = new StickerProcessingJob(stickerId, packageId, request.objectKey());
                                        return jobEmitter.send(job).replaceWith(stickerId);
                                })
                                .map(stickerId -> Response
                                                .status(Response.Status.CREATED)
                                                .entity(stickerId)
                                                .build());
        }

}