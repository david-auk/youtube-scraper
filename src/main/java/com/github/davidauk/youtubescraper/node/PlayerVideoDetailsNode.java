package com.github.davidauk.youtubescraper.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.youtubescraper.model.Thumbnail;
import com.github.davidauk.youtubescraper.model.content.VideoType;
import com.github.davidauk.youtubescraper.util.ThumbnailParser;

import java.util.List;

public record PlayerVideoDetailsNode(JsonNode value) {

    public PlayerVideoDetailsNode {
        if (value == null || value.isMissingNode() || value.isNull()) {
            throw new IllegalArgumentException("videoDetails node is required.");
        }
    }

    public String id() {
        return textAt("videoId");
    }

    public String title() {
        return textAt("title");
    }

    public Integer durationSeconds() {
        String lengthSeconds = textAt("lengthSeconds");

        if (lengthSeconds == null || lengthSeconds.isBlank()) {
            return null;
        }

        return Integer.parseInt(lengthSeconds);
    }

    public List<Thumbnail> thumbnails() {
        return ThumbnailParser.parse(value.path("thumbnail").path("thumbnails"));
    }

    public boolean liveContent() {
        return value.path("isLiveContent").asBoolean(false);
    }

    public VideoType videoType(PlayerMicroformatNode microformat) {
        if (microformat.isCurrentlyLive()) {
            return VideoType.CURRENTLY_LIVE_STREAM;
        }

        if (liveContent() || microformat.wasLiveStream()) {
            return VideoType.PAST_LIVE_STREAM;
        }

        return VideoType.UPLOADED_VIDEO;
    }

    private String textAt(String fieldName) {
        JsonNode node = value.get(fieldName);

        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        return node.asText();
    }
}