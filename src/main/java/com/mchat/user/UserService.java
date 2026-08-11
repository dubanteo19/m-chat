package com.mchat.user;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.mchat.auth.dto.response.UserInfo;
import com.mchat.common.cache.CacheConstants;
import com.mchat.model.User;
import com.mchat.model.json.PushSubscription;
import com.mchat.user.dto.request.UpdateProfileRequest;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserService {
  Logger logger = Logger.getLogger(UserService.class.getName());
  @Inject
  JsonWebToken jwt;

  @Inject
  UserDbService userDbService;

  @Inject
  @CacheName(CacheConstants.USER_INFO_BY_USERNAME)
  Cache userInfoCache;

  public Uni<List<UserInfo>> searchUsersByDisplayName(String displayName) {
    return userDbService.searchUsersByDisplayName(displayName);
  }

  public Uni<User> findByUsername(String username) {
    return userDbService.findByUsername(username);
  }

  @CacheResult(cacheName = CacheConstants.USER_INFO_BY_USERNAME)
  public Uni<UserInfo> getUserInfoByUsername(String username) {
    logger.info("CACHE MISS! Fetching from DB layer for username: " + username);
    return userDbService.findByUsername(username).map(UserInfo::fromEntity);
  }

  public Uni<User> updateProfile(UpdateProfileRequest request) {
    String username = jwt.getName();

    return userDbService.updateProfile(request, username)
        .call(updatedUserInfo -> userInfoCache.invalidate(username));
  }

  public Uni<Void> saveSubscription(PushSubscription subscription) {
    String username = jwt.getName();
    return userDbService.saveSubscription(username, subscription);
  }

}
