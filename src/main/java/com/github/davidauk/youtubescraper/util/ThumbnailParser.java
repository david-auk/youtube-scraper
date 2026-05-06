package com.github.davidauk.youtubescraper.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.youtubescraper.model.Thumbnail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public final class ThumbnailParser {

    private ThumbnailParser() {
    }

    /**
     * Parses a list of thumbnails from a JSON node.
     * @param thumbnailNode the JSON node containing the thumbnail data (in list form with width, height, and url fields)
     * @return a list of Thumbnail objects
     */
    public static List<Thumbnail> parse(JsonNode thumbnailNode) {
        if (thumbnailNode == null || thumbnailNode.isMissingNode() || thumbnailNode.isNull() || !thumbnailNode.isArray() || thumbnailNode.isEmpty()) {
            return null;
        }

        List<Thumbnail> thumbnails = new ArrayList<>();

        for (JsonNode thumbnailJson : thumbnailNode) {
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