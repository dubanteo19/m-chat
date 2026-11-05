package com.mchat.presence;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.microprofile.jwt.Claim;

import com.mchat.socket.UserEventBroadcaster;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/presence")
public class PresenceResource {

    @Inject
    @Claim("userId")
    Long currentUserId;

    @Inject
    UserEventBroadcaster userEventBroadcaster;

    @POST
    public Map<Long, Boolean> getPresence(List<Long> userIds) {
        return userIds.stream()
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        userEventBroadcaster::isOnline));
    }
}