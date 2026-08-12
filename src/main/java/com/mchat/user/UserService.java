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
  UserDbService userDbService;

  @Inject
  @CacheName(CacheConstants.USER_INFO_BY_ID)
  Cache userInfoCache;

  public Uni<List<UserInfo>> searchUsersByDisplayName(String displayName) {
    return userDbService.searchUsersByDisplayName(displayName);
  }

  public Uni<User> findByUsername(String username) {
    return userDbService.findByUsername(username);
  }

  public Uni<UserInfo> getUserInfoByUsername(String username) {
    return userDbService.findIdByUsername(username)
        .chain(userId -> getUserInfoById(userId));
  }

  @CacheResult(cacheName = CacheConstants.USER_INFO_BY_ID)
  public Uni<UserInfo> getUserInfoById(Long userId) {
    logger.info("CACHE MISS! Fetching from DB layer for userId: " + userId);
    return userDbService.findById(userId).map(UserInfo::fromEntity);
  }

  public Uni<User> updateProfile(Long userId, UpdateProfileRequest request) {
    return userDbService.updateProfile(request, userId)
        .call(updatedUserInfo -> userInfoCache.invalidate(userId));
  }

  public Uni<Void> saveSubscription(Long userId, PushSubscription subscription) {
    return userDbService.saveSubscription(userId, subscription);
  }

}
