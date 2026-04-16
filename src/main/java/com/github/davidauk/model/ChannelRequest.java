package com.github.davidauk.model;

import java.net.ProxySelector;
import java.time.Duration;
import java.util.Objects;

public record ChannelRequest(
        String channelId,
        String channelUrl,
        String channelUsername,
        Integer limit,
        Duration sleep,
        ProxySelector proxySelector,
        ChannelSort sortBy,
        ContentType contentType
) {
    public ChannelRequest {
        sleep = sleep == null ? Duration.ofSeconds(1) : sleep;
        sortBy = sortBy == null ? ChannelSort.NEWEST : sortBy;
        contentType = contentType == null ? ContentType.VIDEOS : contentType;

        boolean hasId = channelId != null && !channelId.isBlank();
        boolean hasUrl = channelUrl != null && !channelUrl.isBlank();
        boolean hasUsername = channelUsername != null && !channelUsername.isBlank();

        int count = (hasId ? 1 : 0) + (hasUrl ? 1 : 0) + (hasUsername ? 1 : 0);
        if (count != 1) {
            throw new IllegalArgumentException(
                    "Exactly one of channelId, channelUrl, or channelUsername must be provided."
            );
        }

        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0 when provided.");
        }
    }

    public String baseUrl() {
        if (channelUrl != null && !channelUrl.isBlank()) {
            return channelUrl;
        }
        if (channelId != null && !channelId.isBlank()) {
            return "https://www.youtube.com/channel/" + channelId;
        }
        return "https://www.youtube.com/@" + Objects.requireNonNull(channelUsername);
    }
}