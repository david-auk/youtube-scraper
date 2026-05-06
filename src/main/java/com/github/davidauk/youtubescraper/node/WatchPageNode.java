package com.github.davidauk.youtubescraper.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.youtubescraper.util.JsonNavigator;

public record WatchPageNode(
        String videoId,
        JsonNode initialData,
        JsonNode playerResponse
) {

    public VideoPrimaryInfoRendererNode primaryInfo() {
        return new VideoPrimaryInfoRendererNode(
                JsonNavigator.findFirst(initialData, "videoPrimaryInfoRenderer")
        );
    }

    public VideoSecondaryInfoRendererNode secondaryInfo() {
        return new VideoSecondaryInfoRendererNode(
                JsonNavigator.findFirst(initialData, "videoSecondaryInfoRenderer")
        );
    }

    public PlayerResponseNode player() {
        return new PlayerResponseNode(playerResponse);
    }
}