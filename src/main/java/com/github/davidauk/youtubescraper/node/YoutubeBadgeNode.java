package com.github.davidauk.youtubescraper.node;

import com.fasterxml.jackson.databind.JsonNode;

public record YoutubeBadgeNode(JsonNode value) {

    public boolean hasLabel(String expectedLabel) {
        if (value == null || value.isMissingNode() || !value.isArray()) {
            return false;
        }

        for (JsonNode badge : value) {
            JsonNode renderer = badge.get("metadataBadgeRenderer");
            if (renderer == null || renderer.isNull()) {
                continue;
            }

            if (matches(renderer.get("label"), expectedLabel)) {
                return true;
            }

            if (matches(renderer.path("accessibilityData").path("label"), expectedLabel)) {
                return true;
            }

            if (matches(renderer.get("tooltip"), expectedLabel)) {
                return true;
            }
        }

        return false;
    }

    private boolean matches(JsonNode node, String expectedLabel) {
        return node != null
                && !node.isMissingNode()
                && !node.isNull()
                && expectedLabel.equals(node.asText());
    }
}