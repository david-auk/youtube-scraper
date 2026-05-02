package com.github.davidauk.node;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public record PlayerMicroformatNode(JsonNode value) {

    public PlayerMicroformatNode {
    }

    public Instant publishedAt() {
        if (missing()) {
            return null;
        }

        return parseTimestamp(firstNonBlank(
                textAt("publishDate"),
                textAt("uploadDate")
        ));
    }

    public boolean unlisted() {
        return !missing() && value.path("isUnlisted").asBoolean(false);
    }

    public boolean isCurrentlyLive() {
        if (missing()) {
            return false;
        }

        JsonNode liveBroadcastDetails = value.path("liveBroadcastDetails");

        return !liveBroadcastDetails.isMissingNode()
                && !liveBroadcastDetails.isNull()
                && !liveBroadcastDetails.path("isLiveNow").isMissingNode()
                && liveBroadcastDetails.path("isLiveNow").asBoolean(false);
    }

    public boolean wasLiveStream() {
        if (missing()) {
            return false;
        }

        JsonNode liveBroadcastDetails = value.path("liveBroadcastDetails");

        return !liveBroadcastDetails.isMissingNode()
                && !liveBroadcastDetails.isNull();
    }

    public boolean missing() {
        return value == null || value.isMissingNode() || value.isNull();
    }

    private Instant parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.trim();

        try {
            return Instant.parse(normalizedValue);
        } catch (DateTimeParseException instantException) {
            try {
                return OffsetDateTime.parse(normalizedValue).toInstant();
            } catch (DateTimeParseException offsetDateTimeException) {
                try {
                    return LocalDate.parse(normalizedValue)
                            .atStartOfDay()
                            .toInstant(ZoneOffset.UTC);
                } catch (DateTimeParseException localDateException) {
                    DateTimeParseException parseException = new DateTimeParseException(
                            "Could not parse YouTube timestamp. Expected ISO instant, ISO offset date-time, or ISO local date.",
                            normalizedValue,
                            localDateException.getErrorIndex(),
                            localDateException
                    );

                    parseException.addSuppressed(instantException);
                    parseException.addSuppressed(offsetDateTimeException);
                    throw parseException;
                }
            }
        }
    }

    private String textAt(String fieldName) {
        if (missing()) {
            return null;
        }

        JsonNode node = value.get(fieldName);

        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        return node.asText();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }
}