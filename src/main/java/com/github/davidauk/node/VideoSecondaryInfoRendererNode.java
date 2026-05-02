package com.github.davidauk.node;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Wrapper around YouTube's videoSecondaryInfoRenderer.
 *
 * Responsible for extracting fields like description and channel-related metadata.
 */
public record VideoSecondaryInfoRendererNode(JsonNode value) {

    public VideoSecondaryInfoRendererNode {
        if (value == null || value.isMissingNode() || value.isNull()) {
            throw new IllegalArgumentException("videoSecondaryInfoRenderer node is required.");
        }
    }

    /**
     * Full video description (expanded text block under the video).
     */
    public String description() {
        JsonNode content = value.path("attributedDescription").path("content");
        if (!content.isMissingNode() && !content.isNull() && content.isValueNode()) {
            return content.asText();
        }

        String attributedDescription = new YoutubeTextNode(value.path("attributedDescription")).text();
        if (attributedDescription != null && !attributedDescription.isBlank()) {
            return attributedDescription;
        }

        return new YoutubeTextNode(value.path("description")).text();
    }

    /**
     * Channel name (owner of the video).
     */
    public String channelName() {
        return new YoutubeTextNode(
                value.path("owner").path("videoOwnerRenderer").path("title")
        ).text();
    }

    /**
     * Channel ID of the video owner.
     */
    public String channelId() {
        JsonNode node = value
                .path("owner")
                .path("videoOwnerRenderer")
                .path("navigationEndpoint")
                .path("browseEndpoint")
                .path("browseId");

        if (node.isMissingNode() || node.isNull()) {
            return null;
        }

        return node.asText();
    }

    /**
     * Whether the channel is verified (based on owner badges).
     */
    public boolean channelVerified() {
        return new YoutubeBadgeNode(
                value.path("owner").path("videoOwnerRenderer").path("badges")
        ).hasLabel("Verified");
    }
}
