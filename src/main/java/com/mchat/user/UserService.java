
package com.mchat.user;

import com.mchat.auth.dto.response.UserInfo;
import com.mchat.model.User;
import com.mchat.user.dto.request.UpdateProfileRequest;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class UserService {

  @WithTransaction
  public Uni<User> findByUsername(String username) {
    return User.findByUsername(username);
  }

  @WithTransaction
  public Uni<UserInfo> getUserInfoByUsername(String username) {
    return findByUsername(username)
        .onItem().ifNull().failWith(() -> new NotFoundException("User not found"))
        .onItem().transform(userEntity -> UserInfo.fromEntity(userEntity));
  }

  @WithTransaction
  public Uni<User> updateProfile(String username, UpdateProfileRequest request) {
    return User.findByUsername(username)
        .onItem().ifNull().failWith(new NotFoundException("User not found"))
        .onItem().invoke(user -> {
          if (request.displayName != null && !request.displayName.isBlank()) {
            user.displayName = request.displayName;
          }
          if (request.title != null) {
            user.title = request.title;
          }
          if (request.avatarUrl != null) {
            user.avatarUrl = request.avatarUrl;
          }

          if (request.titleStyle != null) {
            user.titleStyle = request.titleStyle;
          }
        });
  }
}
