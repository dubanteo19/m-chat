package com.mchat.auth;

import java.util.logging.Logger;

import com.mchat.auth.dto.request.UserLoginRequest;
import com.mchat.auth.dto.request.UserRegisterRequest;
import com.mchat.user.UserService;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
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
    Logger logger = Logger.getLogger(AuthResource.class.getName());

    @Inject
    AuthService authService;
    @Inject
    PasswordService passwordService;
    @Inject
    UserService userService;

    @POST
    @Path("/logout")
    public Uni<Response> logout() {
        String cookieHeader = "m_user=; Domain=.dbt19.site; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=Lax";

        return Uni.createFrom()
                .item(() -> Response.ok()
                        .header("Set-Cookie", cookieHeader)
                        .build());
    }

    @POST
    @Path("/register")
    public Uni<Response> register(UserRegisterRequest request) {
        return passwordService
                .hashPassword(request.password())
                .chain(
                        hashedPassword -> {
                            var secureRequest = new UserRegisterRequest(
                                    request.username(), hashedPassword, request.displayName());

                            return authService
                                    .registerUser(secureRequest)
                                    .map(
                                            userInfo -> Response.status(Response.Status.CREATED).entity(userInfo)
                                                    .build());
                        });
    }

    @GET
    @Path("/me")
    public Uni<Response> me(@CookieParam("m_user") String username) {
        if (username == null) {
            logger.warning("No valid session found.");
            return Uni.createFrom().item(
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        return userService.getUserInfoByUsername(username)
                .map(userInfo -> {
                    if (userInfo == null) {
                        return Response.status(Response.Status.NOT_FOUND).build();
                    }
                    logger.info("User info retrieved for username: " + username);
                    return Response.ok(userInfo).build();
                });
    }

    @POST
    @Path("/login")
    public Uni<Response> login(UserLoginRequest request) {
        return authService
                .loginUser(request)
                .map(
                        userInfo -> {
                            String cookieHeader = String.format(
                                    "m_user=%s; Domain=.dbt19.site; Path=/; Max-Age=%d; Secure; HttpOnly; SameSite=Lax",
                                    userInfo.username(),
                                    60 * 60 * 24 * 7);

                            return Response.ok(userInfo)
                                    .header("Set-Cookie", cookieHeader)
                                    .build();
                        });
    }
}
