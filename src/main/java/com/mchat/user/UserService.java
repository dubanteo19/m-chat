package com.mchat.user;

import java.util.List;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.mchat.auth.dto.response.UserInfo;
import com.mchat.model.User;
import com.mchat.model.json.PushSubscription;
import com.mchat.user.dto.request.UpdateProfileRequest;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class UserService {
  @Inject
  JsonWebToken jwt;

  @WithSession
  public Uni<User> findByUsername(String username) {
    return User.findByUsername(username)
        .onItem()
        .ifNull()
        .failWith(() -> new NotFoundException("User not found"));
  }
  @WithTransaction
  public Uni<List<UserInfo>> searchUsersByDisplayName(String displayName) {
    return User.searchByDisplayName(displayName)
        .map(users -> users.stream().map(UserInfo::fromEntity).toList());
  }

  @WithTransaction
  public Uni<UserInfo> getUserInfoByUsername(String username) {
    return findByUsername(username).map(userEntity -> UserInfo.fromEntity(userEntity));
  }

  @WithTransaction
  public Uni<User> updateProfile(UpdateProfileRequest request) {
    String username = jwt.getName();
    return findByUsername(username)
        .invoke(
            user -> {
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

  @WithTransaction
  public Uni<Void> saveSubscription(PushSubscription subscription) {
    String username = jwt.getName();
    return findByUsername(username)
        .invoke(user -> user.pushSubscription = subscription)
        .replaceWithVoid();
  }
}
