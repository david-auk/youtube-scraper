package com.github.davidauk.model;

import com.github.davidauk.client.YoutubeClient;

import java.io.IOException;
import java.util.regex.Pattern;

public class ChannelFactory {

    private static final Pattern CHANNEL_ID_PATTERN = Pattern.compile(
            "UC[A-Za-z0-9_-]{22}"
    );

    private static final Pattern CANONICAL_CHANNEL_PATTERN = Pattern.compile(
            "https?://(www\\.)?youtube\\.com/channel/([A-Za-z0-9_-]+)"
    );

    private static final Pattern USERNAME_CHANNEL_PATTERN = Pattern.compile(
            "https?://(www\\.)?youtube\\.com/@([A-Za-z0-9._-]+)"
    );

    public static Channel buildChannelRequest(String channelDifferentiator, YoutubeClient client) throws IOException, InterruptedException {

        channelDifferentiator = channelDifferentiator.trim();

        if (channelDifferentiator.isEmpty()) {
            throw new IllegalArgumentException("Channel differentiator cannot be empty.");
        }

        // Check if the input format resembles a YouTube channel ID
        if (CHANNEL_ID_PATTERN.matcher(channelDifferentiator).matches()) {
            return createChannelFromId(channelDifferentiator);
        }

        // Try to match the string to a "/channel/ID" URL
        java.util.regex.Matcher canonicalChannelMatcher = CANONICAL_CHANNEL_PATTERN.matcher(channelDifferentiator);
        if (canonicalChannelMatcher.matches()) {
            return createChannelFromId(canonicalChannelMatcher.group(2));
        }

        // Try to match the string to a "@username" URL
        java.util.regex.Matcher usernameChannelMatcher = USERNAME_CHANNEL_PATTERN.matcher(channelDifferentiator);
        if (usernameChannelMatcher.matches()) {
            return createChannelFromUsername(usernameChannelMatcher.group(2), client);
        }

        // Normalize usernames that start with '@' (e.g. "@LinusTechTips")
        if (channelDifferentiator.startsWith("@")) {
            channelDifferentiator = channelDifferentiator.substring(1);
        }

        // Validate username format (should not contain slashes or invalid characters)
        if (!isValidUsername(channelDifferentiator)) {
            throw new IllegalArgumentException("Invalid YouTube username format: " + channelDifferentiator);
        }

        // Default to username
        return createChannelFromUsername(channelDifferentiator, client);
    }

    private static Channel createChannelFromId(String channelId) {
        return new Channel(channelId);
    }

    private static Channel createChannelFromUsername(String username, YoutubeClient client) throws IOException, InterruptedException {
        return createChannelFromId(client.resolveChannelIdFromUsername(username));
    }

    /**
     * Validates whether the given string is a valid YouTube username handle.
     * <p>
     * Rules:
     * - Only letters, numbers, dots, underscores and dashes are allowed
     * - Must not contain slashes or spaces
     */
    private static boolean isValidUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        // Reject obvious invalid patterns early
        if (username.contains("/") || username.contains(" ")) {
            return false;
        }

        // Match allowed username characters
        return username.matches("^[A-Za-z0-9._-]+$");
    }
}