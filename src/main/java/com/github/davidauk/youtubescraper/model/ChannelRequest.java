package com.github.davidauk.youtubescraper.model;

import com.github.davidauk.youtubescraper.model.content.ContentType;

import java.net.ProxySelector;
import java.time.Duration;

public record ChannelRequest(
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

        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0 when provided.");
        }
    }
}