package com.github.davidauk.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidauk.model.content.ContentAvailability;

public record PlayabilityStatusNode(
        JsonNode value,
        PlayerMicroformatNode microformat
) {

    public PlayabilityStatusNode {
        if (value == null || value.isMissingNode() || value.isNull()) {
            throw new IllegalArgumentException("playabilityStatus node is required.");
        }
    }

    public ContentAvailability contentAvailability(VideoPrimaryInfoRendererNode videoPrimaryInfoRenderer) {
        String status = textAt("status");
        String reason = textAt("reason");

        if ("OK".equals(status)) {

            // If the video is members-only, it's not public.
            if (videoPrimaryInfoRenderer.membersOnly()) return ContentAvailability.MEMBERS_ONLY;

            return microformat.unlisted()
                    ? ContentAvailability.UNLISTED
                    : ContentAvailability.PUBLIC;
        }

        String subreason = textAt("errorScreen", "playerErrorMessageRenderer", "subreason", "simpleText");
        String detailedReason = joinNonNull(reason, subreason).toLowerCase();

        if (detailedReason.contains("private")) {
            return ContentAvailability.PRIVATE;
        }

        if (detailedReason.contains("removed")
                || detailedReason.contains("deleted")
                || detailedReason.contains("no longer available")) {
            return ContentAvailability.DELETED;
        }

        return ContentAvailability.PRIVATE;
    }

    private String textAt(String fieldName) {
        JsonNode node = value.get(fieldName);

        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        return node.asText();
    }

    private String textAt(String... fieldNames) {
        JsonNode node = value;

        for (String fieldName : fieldNames) {
            if (node == null || node.isMissingNode() || node.isNull()) {
                return null;
            }

            node = node.get(fieldName);
        }

        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        return node.asText();
    }

    private String joinNonNull(String... values) {
        StringBuilder builder = new StringBuilder();

        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(' ');
            }

            builder.append(value);
        }

        return builder.toString();
    }
}