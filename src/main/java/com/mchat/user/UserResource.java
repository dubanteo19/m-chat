package com.mchat.user;

import com.mchat.user.dto.request.UpdateProfileRequest;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @GET
    @Path("/{username}/profile")
    public Uni<Response> getUserProfile(@PathParam("username") String username) {
        return userService.getUserInfoByUsername(username)
                .onItem().transform(userInfo -> Response.ok(userInfo).build());
    }

    @PUT
    @Path("/{username}/profile")
    public Uni<Response> updateProfile(@PathParam("username") String username, UpdateProfileRequest request) {
        return userService.updateProfile(username, request)
                .onItem().transform(updatedUser -> Response.ok(updatedUser).build());
    }
}