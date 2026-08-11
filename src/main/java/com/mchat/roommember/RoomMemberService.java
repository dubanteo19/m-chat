package com.mchat.roommember;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.mchat.common.cache.CacheConstants;
import com.mchat.model.RoomMember;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RoomMemberService {
  @Inject
  RoomMemberDbService roomMemberDbService;
  @Inject
  JsonWebToken jwt;

  @CacheInvalidate(cacheName = CacheConstants.ROOM_MEMBERS)
  public Uni<RoomMember> inviteUser(@CacheKey String roomId, String username) {
    return roomMemberDbService.inviteUser(roomId, username);
  }

  @CacheInvalidate(cacheName = CacheConstants.ROOM_MEMBERS)
  public Uni<Boolean> kickUser(@CacheKey String roomId, String targetUsername) {
    String actorUsername = jwt.getName();
    return roomMemberDbService.kickUser(actorUsername, targetUsername, roomId);
  }

}
