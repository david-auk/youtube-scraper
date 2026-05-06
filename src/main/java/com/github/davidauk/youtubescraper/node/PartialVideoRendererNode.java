package com.github.davidauk.youtubescraper.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.youtubescraper.model.Thumbnail;
import com.github.davidauk.youtubescraper.util.DurationParser;
import com.github.davidauk.youtubescraper.util.ThumbnailParser;

import java.util.List;

public record PartialVideoRendererNode(JsonNode value) {

    public PartialVideoRendererNode {
        if (value == null || value.isMissingNode() || value.isNull()) {
            throw new IllegalArgumentException("Partial video renderer node is required.");
        }
    }

    public String id() {
        return textAt("videoId");
    }

    public String title() {
        return new YoutubeTextNode(value.get("title")).text();
    }

    public String descriptionSnippet() {
        return new YoutubeTextNode(value.get("descriptionSnippet")).text();
    }

    public Integer durationSeconds() {
        return DurationParser.parseSeconds(
                new YoutubeTextNode(value.get("lengthText")).text()
        );
    }

    public List<Thumbnail> thumbnails() {
        return ThumbnailParser.parse(value.get("thumbnail"));
    }

    public boolean membersOnly() {
        return new YoutubeBadgeNode(value.get("badges")).hasLabel("Members only");
    }

    public boolean verified() {
        return new YoutubeBadgeNode(value.get("ownerBadges")).hasLabel("Verified");
    }

    public boolean hasEnoughData() {
        return id() != null || title() != null;
    }

    private String textAt(String fieldName) {
        JsonNode field = value.get(fieldName);

        if (field == null || field.isMissingNode() || field.isNull()) {
            return null;
        }

        return field.asText();
    }
}