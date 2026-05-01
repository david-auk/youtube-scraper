package com.github.davidauk.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.model.Thumbnail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public final class ThumbnailParser {

    private ThumbnailParser() {
    }

    public static List<Thumbnail> parse(JsonNode thumbnailNode) {
        if (thumbnailNode == null || thumbnailNode.isMissingNode() || thumbnailNode.isNull()) {
            return null;
        }

        JsonNode thumbnailsJson = thumbnailNode.get("thumbnails");

        if (thumbnailsJson == null || !thumbnailsJson.isArray() || thumbnailsJson.isEmpty()) {
            return null;
        }

        List<Thumbnail> thumbnails = new ArrayList<>();

        for (JsonNode thumbnailJson : thumbnailsJson) {
            try {
                thumbnails.add(new Thumbnail(
                        thumbnailJson.get("width").asInt(),
                        thumbnailJson.get("height").asInt(),
                        new URI(thumbnailJson.get("url").asText())
                ));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid thumbnail URL.", e);
            }
        }

        return thumbnails;
    }
}