package com.mchat.auth;

import com.mchat.auth.dto.request.UserLoginRequest;
import com.mchat.auth.dto.request.UserRegisterRequest;
import com.mchat.auth.dto.response.UserResponse;
import com.mchat.exception.InvalidCredentialsException;
import com.mchat.exception.UsernameAlreadyExistsException;
import com.mchat.model.User;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthService {

  @WithTransaction
  public Uni<UserResponse> registerUser(UserRegisterRequest request) {
    return User.findByUsername(request.username())
        .chain(
            existingUser -> {
              if (existingUser != null) {
                return Uni.createFrom()
                    .failure(new UsernameAlreadyExistsException(request.username()));
              }
              var user = new User();
              user.username = request.username();
              user.displayName = request.displayName();
              user.setAndHashPassword(request.password());
              return user.persist().replaceWith(() -> UserResponse.fromEntity(user));
            });
  }

  @WithTransaction
  public Uni<UserResponse> loginUser(UserLoginRequest request) {
    return User.findByUsername(request.username())
        .map(
            user -> {
              if (user == null || !user.checkPassword(request.password())) {
                throw new InvalidCredentialsException();
              }
              return UserResponse.fromEntity(user);
            });
  }
}
