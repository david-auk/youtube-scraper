package com.github.davidauk.selector;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.model.content.ContentType;
import com.github.davidauk.model.Thumbnail;
import com.github.davidauk.model.content.ContentAvailability;
import com.github.davidauk.model.content.PartialVideo;
import com.github.davidauk.model.content.VideoType;
import com.github.davidauk.node.YoutubeBadgeNode;
import com.github.davidauk.util.DurationParser;
import com.github.davidauk.util.ThumbnailParser;

import java.util.List;

public record LockupViewModelSelectorItemNode(JsonNode raw) implements PartialVideoSelectorItemNode {
    @Override
    public PartialVideo toPartialVideo(ContentType contentType) {
        if (!hasEnoughData()) {
            return null;
        }
        ContentAvailability contentAvailability =
                membersOnly() ? ContentAvailability.MEMBERS_ONLY : ContentAvailability.PUBLIC;

        return new PartialVideo(
                id(),
                title(),
                durationSeconds(),
                thumbnails(),
                contentAvailability,
                detectVideoType(contentType)
        );
    }

    public String id() {
        return firstNonBlank(
                textAt(raw, "contentId"),
                textAt(raw.path("rendererContext")
                        .path("commandContext")
                        .path("onTap")
                        .path("innertubeCommand")
                        .path("watchEndpoint"), "videoId")
        );
    }

    public String title() {
        return textAt(raw.path("metadata")
                .path("lockupMetadataViewModel")
                .path("title"), "content");
    }

    public Integer durationSeconds() {
        if (isCurrentlyLive()) {
            return null;
        }

        String duration = firstNonBlank(
                thumbnailBadgeText(),
                thumbnailBadgeAccessibilityLabel()
        );

        return DurationParser.parseSeconds(duration);
    }

    public List<Thumbnail> thumbnails() {
        return ThumbnailParser.parse(raw.path("contentImage")
                .path("thumbnailViewModel")
                .path("image")
                .path("sources")
        );
    }

    public boolean membersOnly() {
        return new YoutubeBadgeNode(raw.path("badges")).hasLabel("Members only")
                || raw.toString().contains("Members only");
    }

    public boolean verified() {
        return new YoutubeBadgeNode(raw.path("ownerBadges")).hasLabel("Verified")
                || raw.toString().contains("BADGE_STYLE_TYPE_VERIFIED")
                || raw.toString().contains("Verified");
    }

    public boolean hasEnoughData() {
        return id() != null || title() != null;
    }

    private VideoType detectVideoType(ContentType contentType) {
        if (contentType == ContentType.VIDEOS) {
            return VideoType.UPLOADED_VIDEO;
        }

        if (contentType == ContentType.STREAMS) {
            if (isCurrentlyLive()) {
                return VideoType.CURRENTLY_LIVE_STREAM;
            }

            return VideoType.PAST_LIVE_STREAM;
        }

        return null;
    }

    private boolean isCurrentlyLive() {
        String value = raw.toString();
        return value.contains("\"text\":\"LIVE\"")
                || value.contains("\"imageName\":\"LIVE\"")
                || value.contains("THUMBNAIL_OVERLAY_BADGE_STYLE_LIVE")
                || value.contains("Upcoming") // TODO Map into own new ContentAvailability type
                || value.contains(" watching");
    }

    private String thumbnailBadgeText() {
        List<JsonNode> badges = raw.findValues("thumbnailBadgeViewModel");
        for (JsonNode badge : badges) {
            String text = textAt(badge, "text");
            if (text != null && !text.equalsIgnoreCase("LIVE")) {
                return text;
            }
        }

        return null;
    }

    private String thumbnailBadgeAccessibilityLabel() {
        List<JsonNode> accessibilityContexts = raw.findValues("accessibilityContext");
        for (JsonNode accessibilityContext : accessibilityContexts) {
            String label = textAt(accessibilityContext, "label");
            if (label != null && label.toLowerCase().contains("second")) {
                return label;
            }
        }

        return null;
    }

    private String metadataRowPartText(int index) {
        JsonNode metadataParts = raw.path("metadata")
                .path("lockupMetadataViewModel")
                .path("metadata")
                .path("contentMetadataViewModel")
                .path("metadataRows")
                .path(0)
                .path("metadataParts");

        if (!metadataParts.isArray() || metadataParts.size() <= index) {
            return null;
        }

        return textAt(metadataParts.path(index).path("text"), "content");
    }

    private String textAt(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        JsonNode field = node.get(fieldName);
        if (field == null || field.isMissingNode() || field.isNull()) {
            return null;
        }

        String value = field.asText(null);
        if (value == null || value.isBlank()) {
            return null;
        }

        return value;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }
}