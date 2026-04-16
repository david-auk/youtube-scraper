package com.github.davidauk.model;

import java.net.ProxySelector;
import java.time.Duration;

public record PlaylistRequest(
        String playlistId,
        Integer limit,
        Duration sleep,
        ProxySelector proxySelector
) {
    public PlaylistRequest {
        if (playlistId == null || playlistId.isBlank()) {
            throw new IllegalArgumentException("playlistId is required.");
        }

        sleep = sleep == null ? Duration.ofSeconds(1) : sleep;

        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0 when provided.");
        }
    }
}
