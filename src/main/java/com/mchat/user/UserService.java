package com.mchat.user;

import java.util.List;
import java.util.logging.Logger;

import com.mchat.auth.dto.response.CurrentUserInfo;
import com.mchat.auth.dto.response.UserInfo;
import com.mchat.common.cache.CacheConstants;
import com.mchat.model.User;
import com.mchat.model.json.PushSubscription;
import com.mchat.user.dto.request.ToggleNotificationsRequest;
import com.mchat.user.dto.request.UpdateProfileRequest;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserService {
  Logger logger = Logger.getLogger(UserService.class.getName());

  @Inject
  UserDbService userDbService;

  public Uni<List<UserInfo>> searchUsersByDisplayName(String displayName) {
    return userDbService.searchUsersByDisplayName(displayName);
  }

  public Uni<User> findByUsername(String username) {
    return userDbService.findByUsername(username);
  }

  public Uni<CurrentUserInfo> getCurrentUserInfo(String username) {
    return userDbService.findIdByUsername(username)
        .chain(userId -> getCurrentUserInfoById(userId));
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

  @CacheResult(cacheName = CacheConstants.CURRENT_USER_INFO_BY_ID)
  public Uni<CurrentUserInfo> getCurrentUserInfoById(Long userId) {
    logger.info("CACHE MISS! Fetching from DB layer for current user userId: " + userId);
    return userDbService.findById(userId).map(CurrentUserInfo::fromEntity);
  }

  @CacheInvalidate(cacheName = CacheConstants.USER_INFO_BY_ID)
  public Uni<User> updateProfile(@CacheKey Long userId, UpdateProfileRequest request) {
    return userDbService.updateProfile(request, userId);
  }

  @CacheInvalidate(cacheName = CacheConstants.CURRENT_USER_INFO_BY_ID)
  public Uni<CurrentUserInfo> updateNotificationSettings(@CacheKey Long userId, ToggleNotificationsRequest request) {
    return userDbService.updateNotificationSettings(userId, request.allowNotify())
        .map(CurrentUserInfo::fromEntity);
  }

  public Uni<Void> saveSubscription(Long userId, PushSubscription subscription) {
    return userDbService.saveSubscription(userId, subscription);
  }

}
