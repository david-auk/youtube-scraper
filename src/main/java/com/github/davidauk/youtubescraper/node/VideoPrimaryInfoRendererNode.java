package com.github.davidauk.youtubescraper.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.youtubescraper.model.Thumbnail;
import com.github.davidauk.youtubescraper.util.JsonNavigator;
import com.github.davidauk.youtubescraper.util.ThumbnailParser;

import java.time.LocalDate;
import java.util.List;

public record VideoPrimaryInfoRendererNode(JsonNode value) {

    public VideoPrimaryInfoRendererNode {
    }

    public String title() {
        if (missing()) {
            return null;
        }
        return new YoutubeTextNode(value.get("title")).text();
    }

    public LocalDate publishedAt() {
        if (missing()) {
            return null;
        }

        String text = new YoutubeTextNode(value.get("dateText")).text();
        if (text == null) {
            return null;
        }

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH);
        return LocalDate.parse(text, formatter);
    }

    public String description() {
        if (missing()) {
            return null;
        }

        return new YoutubeTextNode(
                JsonNavigator.findFirst(value, "attributedDescriptionBodyText")
        ).text();
    }

    public Integer durationSeconds() {
        return null;
    }

    public List<Thumbnail> thumbnails() {
        if (missing()) {
            return List.of();
        }
        return ThumbnailParser.parse(value.get("thumbnail"));
    }

    public boolean membersOnly() {
        return !missing() && new YoutubeBadgeNode(value.get("badges")).hasLabel("Members only");
    }

    public boolean verified() {
        return !missing() && new YoutubeBadgeNode(value.get("ownerBadges")).hasLabel("Verified");
    }

    public boolean missing() {
        return value == null || value.isMissingNode() || value.isNull();
    }
}