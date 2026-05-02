package com.github.davidauk.node;

import com.fasterxml.jackson.databind.JsonNode;

public record PlayerResponseNode(JsonNode value) {

    public PlayerResponseNode {
        if (value == null || value.isMissingNode() || value.isNull()) {
            throw new IllegalArgumentException("player node is required.");
        }
    }

    public PlayerVideoDetailsNode videoDetails() {
        return new PlayerVideoDetailsNode(value.path("videoDetails"));
    }

    public PlayerMicroformatNode microformat() {
        return new PlayerMicroformatNode(
                value.path("microformat").path("playerMicroformatRenderer")
        );
    }

    public PlayabilityStatusNode playabilityStatus() {
        return new PlayabilityStatusNode(value.path("playabilityStatus"), microformat());
    }
}