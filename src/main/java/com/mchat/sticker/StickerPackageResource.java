package com.mchat.sticker;

import com.mchat.sticker.dto.request.CreateStickerPackageRequest;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/sticker-packages")
@RequestScoped
public class StickerPackageResource {

        @POST
        public Uni<Response> createPackage(CreateStickerPackageRequest request) {
                throw new UnsupportedOperationException("Not implemented yet");
        }

        @POST
        @Path("/{packageId}/stickers")
        public Uni<Response> addSticker(
                        @PathParam("packageId") Long packageId) {
                throw new UnsupportedOperationException("Not implemented yet");
        }

}