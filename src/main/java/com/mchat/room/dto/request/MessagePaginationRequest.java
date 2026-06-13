package com.mchat.room.dto.request;

import jakarta.ws.rs.QueryParam;
import java.time.Instant;

public record MessagePaginationRequest(@QueryParam("before") Instant before) {
  private static final int FIXED_LIMIT = 20;

  public Integer getLimit() {
    return FIXED_LIMIT;
  }
}
