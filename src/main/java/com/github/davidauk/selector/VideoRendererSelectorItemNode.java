package com.github.davidauk.selector;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.model.Thumbnail;
import com.github.davidauk.model.content.*;
import com.github.davidauk.node.YoutubeBadgeNode;
import com.github.davidauk.node.YoutubeTextNode;
import com.github.davidauk.util.DurationParser;
import com.github.davidauk.util.ThumbnailParser;

import java.util.List;

public record VideoRendererSelectorItemNode(JsonNode raw) implements PartialVideoSelectorItemNode {
    @Override
    public PartialVideo toPartialVideo(ContentType contentType) {

        System.out.println("VideoRendererSelectorItemNode.toPartialVideo");

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
        JsonNode field = raw.get("videoId");

        if (field == null || field.isMissingNode() || field.isNull()) {
            return null;
        }

        return field.asText();
    }

    public String title() {
        return new YoutubeTextNode(raw.get("title")).text();
    }

    public Integer durationSeconds() {
        return DurationParser.parseSeconds(
                new YoutubeTextNode(raw.get("lengthText")).text()
        );
    }

    public List<Thumbnail> thumbnails() {
        return ThumbnailParser.parse(raw.get("thumbnail").get("thumbnails"));
    }

    public boolean membersOnly() {
        return new YoutubeBadgeNode(raw.get("badges")).hasLabel("Members only");
    }

    public boolean verified() {
        return new YoutubeBadgeNode(raw.get("ownerBadges")).hasLabel("Verified");
    }

    public boolean hasEnoughData() {
        return id() != null || title() != null;
    }

    private VideoType detectVideoType(ContentType contentType) {

        if (contentType == ContentType.VIDEOS) {
            return VideoType.UPLOADED_VIDEO;
        } else if (contentType == ContentType.STREAMS) {
            String value = raw.toString();

            if (value.contains("\"style\":\"LIVE\"")
                    || value.contains("\"iconType\":\"LIVE\"")
                    || value.contains(" watching")) {
                return VideoType.CURRENTLY_LIVE_STREAM;
            }

            return VideoType.PAST_LIVE_STREAM;
        }

        return null;
    }
}