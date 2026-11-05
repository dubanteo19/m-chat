package com.mchat.room;

import java.util.List;

import com.mchat.roommember.dto.response.RoomMemberInfo;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RoomCacheService {

    @CacheName("room-members")
    Cache cache;

    @SuppressWarnings("unchecked")
    public List<RoomMemberInfo> getCachedMembers(String roomId) {
        var future = cache.as(CaffeineCache.class).getIfPresent(roomId);

        if (future == null || !future.isDone() || future.isCompletedExceptionally()) {
            return null;
        }

        return (List<RoomMemberInfo>) future.getNow(null);
    }
}