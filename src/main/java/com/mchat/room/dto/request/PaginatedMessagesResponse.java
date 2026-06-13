package com.mchat.room.dto.request;

import java.time.Instant;
import java.util.List;

public record PaginatedMessagesResponse<T>(List<T> data, Instant nextCursor, boolean hasMore) {}
