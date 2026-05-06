package com.github.davidauk.youtubescraper.util;

public final class DurationParser {

    private DurationParser() {
    }

    public static Integer parseSeconds(String duration) {
        if (duration == null || duration.isBlank()) {
            return null;
        }

        String[] parts = duration.trim().split(":");

        if (parts.length < 1 || parts.length > 3) {
            throw new IllegalArgumentException("Unsupported duration format: " + duration);
        }

        int seconds = 0;
        int multiplier = 1;

        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];

            if (part.isBlank()) {
                throw new IllegalArgumentException("Unsupported duration format: " + duration);
            }

            try {
                seconds += Integer.parseInt(part) * multiplier;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unsupported duration format: " + duration, e);
            }

            multiplier *= 60;
        }

        return seconds;
    }
}