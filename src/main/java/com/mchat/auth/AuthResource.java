package com.mchat.auth;

import java.time.Duration;
import java.util.logging.Logger;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.mchat.auth.dto.request.UserLoginRequest;
import com.mchat.auth.dto.request.UserRegisterRequest;
import com.mchat.user.UserService;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@RequestScoped
public class AuthResource {
        Logger logger = Logger.getLogger(AuthResource.class.getName());

        @Inject
        AuthService authService;
        @Inject
        PasswordService passwordService;
        @Inject
        UserService userService;
        @Inject
        JsonWebToken jwt;

        @POST
        @Path("/register")
        public Uni<Response> register(UserRegisterRequest request) {
                return passwordService
                                .hashPassword(request.password())
                                .chain(
                                                hashedPassword -> {
                                                        var secureRequest = new UserRegisterRequest(
                                                                        request.username(), hashedPassword,
                                                                        request.displayName());

                                                        return authService
                                                                        .registerUser(secureRequest)
                                                                        .map(
                                                                                        userInfo -> Response.status(
                                                                                                        Response.Status.CREATED)
                                                                                                        .entity(userInfo)
                                                                                                        .build());
                                                });
        }

        @GET
        @Path("/me")
        public Uni<Response> me() {
                String username = jwt.getName();
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
                return authService.loginUser(request)
                                .map(userInfo -> {
                                        String signedToken = Jwt.issuer("https://dbt19.site")
                                                        .upn(userInfo.username())
                                                        .subject(String.valueOf(userInfo.id()))
                                                        .claim("userId", userInfo.id())
                                                        .groups("USER")
                                                        .expiresIn(Duration.ofDays(30))
                                                        .sign();

                                        NewCookie authCookie = new NewCookie.Builder("m_user")
                                                        .value(signedToken)
                                                        .domain("dbt19.site")
                                                        .path("/")
                                                        .maxAge((int) Duration.ofDays(30).toSeconds())
                                                        .secure(false)
                                                        .httpOnly(true)
                                                        .sameSite(NewCookie.SameSite.LAX)
                                                        .build();

                                        return Response.ok(userInfo)
                                                        .cookie(authCookie)
                                                        .build();
                                });
        }

        @POST
        @Path("/logout")
        public Uni<Response> logout() {
                NewCookie authCookie = new NewCookie.Builder("m_user")
                                .path("/")
                                .domain("dbt19.site")
                                .maxAge(0)
                                .secure(false)
                                .httpOnly(true)
                                .sameSite(NewCookie.SameSite.LAX)
                                .build();

                return Uni.createFrom()
                                .item(() -> Response.ok()
                                                .cookie(authCookie)
                                                .build());
        }
}
