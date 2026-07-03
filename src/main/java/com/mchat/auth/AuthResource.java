package com.mchat.auth;

import com.mchat.auth.dto.request.UserLoginRequest;
import com.mchat.auth.dto.request.UserRegisterRequest;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

  @Inject
  AuthService authService;
  @Inject
  PasswordService passwordService;

  @POST
  @Path("/register")

  @Blocking
  public Uni<Response> register(UserRegisterRequest request) {
    // 1. Call a dedicated password service to hash the password safely
    return passwordService.hashPassword(request.password())
        .chain(hashedPassword -> {
          var secureRequest = new UserRegisterRequest(
              request.username(),
              hashedPassword,
              request.displayName());

          // 2. Pass control to your transactional database service
          return authService.registerUser(secureRequest)
              .map(userInfo -> Response.status(Response.Status.CREATED).entity(userInfo).build());
        });
  }

  @POST
  @Path("/login")
  public Uni<Response> login(UserLoginRequest request) {
    return authService.loginUser(request).map(userInfo -> Response.ok(userInfo).build());
  }
}
