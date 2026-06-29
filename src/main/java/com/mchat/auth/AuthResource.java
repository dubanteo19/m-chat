package com.mchat.auth;

import com.mchat.auth.dto.request.UserLoginRequest;
import com.mchat.auth.dto.request.UserRegisterRequest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

  @Inject AuthService authService;

  @POST
  @Path("/register")
  public Uni<Response> register(UserRegisterRequest request) {
    return authService
        .registerUser(request)
        .map(userInfo -> Response.status(Response.Status.CREATED).entity(userInfo).build());
  }

  @POST
  @Path("/login")
  public Uni<Response> login(UserLoginRequest request) {
    System.out.println("reeice request"+ request);
    return authService.loginUser(request).map(userInfo -> Response.ok(userInfo).build());
  }
}
