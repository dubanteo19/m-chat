package com.mchat.sticker;

import org.eclipse.microprofile.jwt.Claim;

import com.mchat.sticker.dto.request.CreateStickerPackageRequest;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
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

        @POST
        public Uni<Response> createPackage(CreateStickerPackageRequest request) {
                return stickerPackageService.create(currentUserId, request)
                                .map(packageId -> Response.status(Response.Status.CREATED).entity(packageId).build());
        }

        @POST
        @Path("/{packageId}/stickers")
        public Uni<Response> addSticker(
                        @PathParam("packageId") Long packageId) {
                throw new UnsupportedOperationException("Not implemented yet");
        }

}