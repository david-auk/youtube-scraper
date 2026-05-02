package com.github.davidauk.node;

import com.fasterxml.jackson.databind.JsonNode;

public record YoutubeTextNode(JsonNode value) {

    public String text() {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }

        JsonNode simpleText = value.get("simpleText");
        if (simpleText != null && !simpleText.isNull()) {
            return simpleText.asText();
        }

        JsonNode runs = value.get("runs");
        if (runs != null && runs.isArray() && !runs.isEmpty()) {
            StringBuilder builder = new StringBuilder();

            for (JsonNode run : runs) {
                JsonNode text = run.get("text");
                if (text != null && !text.isNull()) {
                    if (!builder.isEmpty()) {
                        builder.append(' ');
                    }

                    builder.append(text.asText());
                }
            }

            return builder.isEmpty() ? null : builder.toString();
        }

        JsonNode accessibilityLabel = value.path("accessibility")
                .path("accessibilityData")
                .path("label");

        if (!accessibilityLabel.isMissingNode() && !accessibilityLabel.isNull()) {
            return accessibilityLabel.asText();
        }

        return null;
    }
}